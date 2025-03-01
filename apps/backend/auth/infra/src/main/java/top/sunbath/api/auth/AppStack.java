package top.sunbath.api.auth;

import io.micronaut.aws.cdk.function.MicronautFunction;
import io.micronaut.aws.cdk.function.MicronautFunctionFile;
import io.micronaut.starter.application.ApplicationType;
import io.micronaut.starter.options.BuildTool;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.aws_apigatewayv2_integrations.HttpLambdaIntegration;
import software.amazon.awscdk.services.apigatewayv2.*;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppStack extends Stack {

    public AppStack(final Construct parent, final String id) {
        this(parent, id, null);
    }

    public AppStack(final Construct parent, final String id, final StackProps props) {
        super(parent, id, props);

        var serviceName = "auth";

        // 创建 DynamoDB 表
        Table usersTable = Table.Builder.create(this, "UsersTable")
                .tableName("users")
                .partitionKey(Attribute.builder()
                        .name("id")
                        .type(AttributeType.STRING)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST) // 按需计费模式，适合开发和低流量场景
                .removalPolicy(software.amazon.awscdk.RemovalPolicy.RETAIN) // 保留表，防止意外删除
                .build();

        Map<String, String> environmentVariables = new HashMap<>();
        // 设置生产环境
        environmentVariables.put("MICRONAUT_ENVIRONMENTS", "production");
        // 设置 DynamoDB 表名
        environmentVariables.put("DYNAMODB_TABLE_NAME", usersTable.getTableName());

        var function = MicronautFunction.create(ApplicationType.DEFAULT,
                        false,
                        this,
                        serviceName + "-function")
                .runtime(Runtime.JAVA_21)
                .handler("io.micronaut.function.aws.proxy.payload2.APIGatewayV2HTTPEventFunction")
                .environment(environmentVariables)
                .code(Code.fromAsset(functionPath()))
                .timeout(Duration.seconds(10))
                .memorySize(512)
                .logRetention(RetentionDays.ONE_WEEK)
                .tracing(Tracing.ACTIVE)
                .architecture(Architecture.X86_64)
                .snapStart(SnapStartConf.ON_PUBLISHED_VERSIONS)
                .build();

        // 授予 Lambda 函数对 DynamoDB 表的读写权限
        usersTable.grantReadWriteData(function);
        
        // 额外授予 Lambda 函数创建和管理索引的权限
        function.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(Arrays.asList(
                        "dynamodb:UpdateTable",
                        "dynamodb:DescribeTable",
                        "dynamodb:CreateTable"
                ))
                .resources(Arrays.asList(
                        usersTable.getTableArn()
                ))
                .build());

        var currentVersion = function.getCurrentVersion();
        var prodAlias = Alias.Builder.create(this, "ProdAlias")
                .aliasName("Prod")
                .version(currentVersion)
                .build();

        HttpLambdaIntegration lambdaIntegration = HttpLambdaIntegration.Builder
                .create("LambdaIntegration", prodAlias)
                .build();

        HttpApi httpApi = HttpApi.Builder.create(this, serviceName + "-http-api")
                .defaultIntegration(lambdaIntegration)
                .corsPreflight(CorsPreflightOptions.builder()
                        .allowOrigins(Arrays.asList("https://sunbath.top", "http://localhost:4200"))
                        .allowMethods(Arrays.asList(
                                CorsHttpMethod.GET,
                                CorsHttpMethod.POST,
                                CorsHttpMethod.PUT,
                                CorsHttpMethod.DELETE,
                                CorsHttpMethod.OPTIONS
                        ))
                        .allowHeaders(Arrays.asList("Content-Type", "Authorization", "X-Amz-Date", "X-Api-Key"))
                        .allowCredentials(true)
                        .maxAge(Duration.days(1))
                        .build())
                .build();

        // 配置自定义域名
        var domainName = "api.sunbath.top";
        var basePath = "auth";
        var domainHostedZoneId = "ZL327KTPIQFUL";
        var domainAlias = "d-she55i1zs4.execute-api.ap-southeast-1.amazonaws.com";
        var url = "https://" + domainName + "/" + basePath;

        var domainNameV2 = DomainName.fromDomainNameAttributes(
                this,
                "ApiDomainNameV2",
                DomainNameAttributes.builder()
                        .name(domainName)
                        .regionalHostedZoneId(domainHostedZoneId)
                        .regionalDomainName(domainAlias)
                        .build()
        );

        ApiMapping.Builder.create(this, serviceName + "-api-mapping")
                .api(httpApi)
                .domainName(domainNameV2)
                .apiMappingKey(basePath)
                .stage(httpApi.getDefaultStage())
                .build();

        // 输出 DynamoDB 表名
        CfnOutput.Builder.create(this, "UsersTableName")
                .exportName("UsersTableName")
                .value(usersTable.getTableName())
                .build();

        CfnOutput.Builder.create(this, "AuthApiUrl")
                .exportName("AuthApiUrl")
                .value(url)
                .build();
    }

    public static String functionPath() {
        return "../app/build/libs/" + functionFilename();
    }

    public static String functionFilename() {
        return MicronautFunctionFile.builder()
                .optimized()
                .graalVMNative(false)
                .version("0.1")
                .archiveBaseName("app")
                .buildTool(BuildTool.GRADLE)
                .build();
    }
} 