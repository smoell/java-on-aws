package com.unicorn;

import java.util.Arrays;

import com.unicorn.constructs.WorkshopVpc;
import com.unicorn.constructs.VSCodeIde;
import com.unicorn.constructs.VSCodeIde.VSCodeIdeProps;
import com.unicorn.constructs.CodeBuildResource;
import com.unicorn.constructs.CodeBuildResource.CodeBuildResourceProps;
import com.unicorn.constructs.EksCluster;
import com.unicorn.core.*;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.quicksight.CfnTheme;
import software.amazon.awscdk.services.sns.subscriptions.EmailSubscription;
import software.constructs.Construct;

public class UnicornStoreStack extends Stack {

    private final String bootstrapScript = """
        date

        echo '=== Clone Git repository ==='
        sudo -H -u ec2-user bash -c "git clone https://github.com/aws-samples/java-on-aws ~/java-on-aws/"
        # sudo -H -u ec2-user bash -c "cd ~/java-on-aws && git checkout refactoring"

        echo '=== Setup IDE ==='
        sudo -H -i -u ec2-user bash -c "~/java-on-aws/infrastructure/scripts/setup/ide.sh"

        echo '=== Additional Setup ==='
        sudo -H -i -u ec2-user bash -c "~/java-on-aws/infrastructure/scripts/setup/app.sh"
        sudo -H -i -u ec2-user bash -c "~/java-on-aws/infrastructure/scripts/setup/eks.sh"
        """;

    private final String buildspec = """
        version: 0.2
        env:
            shell: bash
        phases:
            install:
                commands:
                    - |
                        aws --version
            build:
                commands:
                    - |
                        # Resolution for when creating the first service in the account
                        aws iam create-service-linked-role --aws-service-name ecs.amazonaws.com 2>/dev/null || true
                        aws iam create-service-linked-role --aws-service-name apprunner.amazonaws.com 2>/dev/null || true
                        aws iam create-service-linked-role --aws-service-name elasticloadbalancing.amazonaws.com 2>/dev/null || true
        """;

    public UnicornStoreStack(final Construct scope, final String id) {
        // super(scope, id, props);
        super(scope, id, StackProps.builder()
        .synthesizer(new DefaultStackSynthesizer(DefaultStackSynthesizerProps.builder()
            .generateBootstrapVersionRule(false)  // This disables the bootstrap version parameter
            .build()))
        .build());

        // Create VPC
        var vpc = new WorkshopVpc(this, "UnicornStoreVpc", "unicornstore-vpc").getVpc();

        // Create VSCode IDE
        var ideProps = new VSCodeIdeProps();
            ideProps.setBootstrapScript(bootstrapScript);
            ideProps.setVpc(vpc);
            ideProps.setInstanceName("unicornstore-ide");
            ideProps.setInstanceType(InstanceType.of(InstanceClass.T3, InstanceSize.MEDIUM));
            ideProps.setExtensions(Arrays.asList(
                // "amazonwebservices.aws-toolkit-vscode",
                // "amazonwebservices.amazon-q-vscode",
                "ms-azuretools.vscode-docker",
                "ms-kubernetes-tools.vscode-kubernetes-tools",
                "vscjava.vscode-java-pack"
            ));
        var ide = new VSCodeIde(this, "UnicornStoreIde", ideProps);
        var ideRole = ideProps.getRole();
        var ideInternalSecurityGroup = ide.getIdeInternalSecurityGroup();

        // Create S3 bucket for thread dumps
        AnalysisBucketConstruct analysisBucket = new AnalysisBucketConstruct(
                this,
                "ThreadDumpBucket",
                AnalysisBucketProps.builder()
                        .bucketPrefix("unicorn-analysis")
                        .versioningEnabled(true)
                        .retentionDays(90)
                        .noncurrentVersionRetentionDays(30)
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .build()
        );

        // Create Lambda function to create thread dump
        InfrastructureLambdaBedrock lambdaBedrock = new InfrastructureLambdaBedrock(this, "InfrastructureLambdaBedrock", this.getRegion(), analysisBucket.getBucket());

        // Create AMP and AMG monitoring infrastructure
        var monitoring = new MonitoringConstruct(this, "UnicornStoreMonitoring", vpc, lambdaBedrock.getThreadDumpFunction());
        // Create Grafana dashboards
        new GrafanaDashboardConstruct(this, "GrafanaDashboards",
                monitoring.getGrafanaWorkspace(), monitoring.getAmpWorkspace());

        // Create Core infrastructure
        var infrastructureCore = new InfrastructureCore(this, "InfrastructureCore", vpc);
        // var accountId = Stack.of(this).getAccount();

        // Create additional infrastructure for Containers modules of Java on AWS Immersion Day
        new InfrastructureContainers(this, "InfrastructureContainers", infrastructureCore);

        // Create EKS cluster for the workshop
        var eksCluster = new EksCluster(this, "UnicornStoreEksCluster", "unicorn-store", "1.32",
            vpc, ideInternalSecurityGroup);
        eksCluster.createAccessEntry(ideRole.getRoleArn(), "unicorn-store", "unicornstore-ide-user");

        // Execute Database setup
        var databaseSetup = new DatabaseSetup(this, "UnicornStoreDatabaseSetup", infrastructureCore);
        databaseSetup.getNode().addDependency(infrastructureCore.getDatabase());

        String bucketName = analysisBucket.getBucket().getBucketName();

        // Create UnicornStoreSpringLambda
        new UnicornStoreSpringLambda(this, "UnicornStoreSpringLambda", infrastructureCore);

        // Create Workshop CodeBuild
        var codeBuildProps = new CodeBuildResourceProps();
        codeBuildProps.setProjectName("unicornstore-codebuild");
        codeBuildProps.setBuildspec(buildspec);
        codeBuildProps.setVpc(vpc);
        codeBuildProps.setAdditionalIamPolicies(Arrays.asList(
            ManagedPolicy.fromAwsManagedPolicyName("AdministratorAccess")));
        new CodeBuildResource(this, "UnicornStoreCodeBuild", codeBuildProps);
    }
}
