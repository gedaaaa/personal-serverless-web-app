package top.sunbath.api;

import io.micronaut.aws.cdk.function.MicronautFunction;
import io.micronaut.aws.cdk.function.MicronautFunctionFile;
import io.micronaut.starter.application.ApplicationType;
import io.micronaut.starter.options.BuildTool;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.BasePathMapping;
import software.amazon.awscdk.services.apigateway.DomainName;
import software.amazon.awscdk.services.apigateway.DomainNameAttributes;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

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
        var function = MicronautFunction.create(ApplicationType.DEFAULT,
                        false,
                        this,
                        serviceName + "-function")
                .runtime(Runtime.JAVA_21)
                .handler("io.micronaut.function.aws.proxy.payload1.ApiGatewayProxyRequestEventFunction")
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
        var api = LambdaRestApi.Builder.create(this, serviceName + "-api")
                .handler(prodAlias)
                .build();

        // 配置自定义域名
        var domainName = "api.sunbath.top"; // 替换为你的域名
        var basePath = "hello-world";
        var url = "https://" + domainName + "/" + basePath;

        // 创建路径映射
        var apiDomainName = DomainName.fromDomainNameAttributes(this, "ApiDomainName",
                DomainNameAttributes.builder()
                        .domainName(domainName)
                        .build());

        BasePathMapping.Builder.create(this, serviceName + "-path-mapping")
                .domainName(apiDomainName)
                .basePath(basePath)
                .restApi(api)
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