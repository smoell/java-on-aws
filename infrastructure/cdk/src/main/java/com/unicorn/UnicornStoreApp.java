package com.unicorn;

import java.util.List;

// import com.unicorn.alternatives.UnicornAuditService;
// import com.unicorn.alternatives.UnicornStoreMicronaut;
// import com.unicorn.alternatives.UnicornStoreQuarkus;
// import com.unicorn.alternatives.UnicornStoreSpringGraalVM;
// import com.unicorn.core.InfrastructureStack;
import io.github.cdklabs.cdknag.AwsSolutionsChecks;
import io.github.cdklabs.cdknag.NagPackSuppression;
import io.github.cdklabs.cdknag.NagSuppressions;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Aspects;
// import software.amazon.awscdk.StackProps;

public class UnicornStoreApp {

    public static void main(final String[] args) {
        App app = new App();

        var unicornStoreStack = new UnicornStoreStack(app, "unicornstore-stack");

        // var infrastructureStack = new InfrastructureStack(app, "UnicornStoreInfrastructure", StackProps.builder()
        //         .build());

        // var unicornStoreSpring = new UnicornStoreStack(app, "UnicornStoreSpringApp", StackProps.builder()
        //         .build(), infrastructureStack);

        // var unicornStoreMicronaut = new UnicornStoreMicronaut(app, "UnicornStoreMicronautApp", StackProps.builder()
        //         .build(), infrastructureStack);

        // var unicornStoreSpringGraalVM = new UnicornStoreSpringGraalVM(app, "UnicornStoreSpringGraalVMApp", StackProps.builder()
        //         .build(), infrastructureStack);

        // var unicornStoreQuarkus = new UnicornStoreQuarkus(app, "UnicornStoreQuarkusApp", StackProps.builder()
        //         .build(), infrastructureStack);

        // var unicornAuditService = new UnicornAuditService(app, "UnicornAuditServiceApp", StackProps.builder()
        //         .build(), infrastructureStack);


        //Add CDK-NAG checks: https://github.com/cdklabs/cdk-nag
        //Add suppression to exclude certain findings that are not needed for Workshop environment
        Aspects.of(app).add(new AwsSolutionsChecks());
        var suppression = List.of(
                new NagPackSuppression.Builder().id("AwsSolutions-APIG4").reason("The workshop environment does not require API-Gateway authorization").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-COG4").reason("The workshop environment does not require Cognito User Pool authorization").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-RDS3").reason("Workshop environment does not need a Multi-AZ setup to reduce cost").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-IAM4").reason("AWS Managed policies are acceptable for the workshop").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-IAM5").reason("A wildcard is acceptable for this workshop to allow parallel creation of resources").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-RDS10").reason("Workshop environment is ephemeral and the database should be deleted by the end of the workshop").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-RDS11").reason("Database is in a private subnet and can use the default port").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-APIG2").reason("API Gateway request validation is not needed for workshop").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-APIG1").reason("API Gateway access logging not needed for workshop setup").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-APIG6").reason("API Gateway access logging not needed for workshop setup").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-VPC7").reason("Workshop environment does not need VPC flow logs").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-SMG4").reason("Ephemeral workshop environment does not need to rotate secrets").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-RDS2").reason("Workshop non-sensitive test database does not need encryption at rest").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-APIG3").reason("Workshop API Gateways do not need AWS WAF assigned").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-EC23").reason("Not needed").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-RDS13").reason("Workshop Database does not need backups").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-S1").reason("Workshop S3 bucket does not need Access Logs").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-L1").reason("Workshop environment use CDK default Lambdas" ).build(),
                new NagPackSuppression.Builder().id("AwsSolutions-RDS6").reason("Workshop environment uses user/password authentication").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-EC28").reason("Workshop instance doesn't need autoscaling").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-EC29").reason("Workshop instance doesn't need autoscaling").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-CFR1").reason("Workshop environment should be accessible from any Geo").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-CFR2").reason("Ephemeral workshop environment does not need WAF").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-CFR3").reason("Ephemeral workshop environment does not need logging").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-CFR4").reason("Workshop instance uses http").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-CFR5").reason("Workshop instance uses http").build(),
                new NagPackSuppression.Builder().id("AwsSolutions-EKS1").reason("Workshop non-sensitive EKS cluster uses public access" ).build(),
                new NagPackSuppression.Builder().id("CdkNagValidationFailure").reason("Suppress warnings see: https://github.com/cdklabs/cdk-nag/issues/817").build()
        );

        NagSuppressions.addStackSuppressions(unicornStoreStack, suppression);
        // NagSuppressions.addStackSuppressions(infrastructureStack, suppression);
        // NagSuppressions.addStackSuppressions(unicornStoreSpring, suppression);
        // NagSuppressions.addStackSuppressions(unicornStoreMicronaut, suppression);
        // NagSuppressions.addStackSuppressions(unicornStoreSpringGraalVM, suppression);
        // NagSuppressions.addStackSuppressions(unicornStoreQuarkus, suppression);
        // NagSuppressions.addStackSuppressions(unicornAuditService, suppression);

        app.synth();
    }
}