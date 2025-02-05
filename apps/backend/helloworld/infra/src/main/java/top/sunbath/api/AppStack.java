package top.sunbath.api;

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
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AppStack extends Stack {


    public AppStack(final Construct parent, final String id) {
        this(parent, id, null);
    }

    public AppStack(final Construct parent, final String id, final StackProps props) {
        super(parent, id, props);

        var serviceName = "hello-world";

        Map<String, String> environmentVariables = new HashMap<>();
        // TODO: use environment to get param from system manager
        // String environment = this.getNode().tryGetContext("environment").toString();
        environmentVariables.put("MICRONAUT_ENVIRONMENTS", "production");


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
                                CorsHttpMethod.OPTIONS // 显式添加OPTIONS
                        ))
                        .allowHeaders(Arrays.asList("Content-Type", "Authorization", "X-Amz-Date", "X-Api-Key"))
                        .allowCredentials(true) // 允许携带凭证
                        .maxAge(Duration.days(1))
                        .build())
                .build();

        // 配置自定义域名
        var domainName = "api.sunbath.top"; // 替换为你的域名
        var basePath = "hello-world";
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
                .apiMappingKey(basePath) // 对应原basePath
                .stage(httpApi.getDefaultStage()) // 关联默认阶段
                .build();

        CfnOutput.Builder.create(this, "MnApiUrl")
                .exportName("MnApiUrl")
                .value(url)
                .build();

        CfnOutput.Builder.create(this, "MnTestApiUrl")
                .exportName("MnTestApiUrl")
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