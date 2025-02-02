package top.sunbath.api;

import io.micronaut.aws.cdk.function.MicronautFunction;
import io.micronaut.aws.cdk.function.MicronautFunctionFile;
import io.micronaut.starter.application.ApplicationType;
import io.micronaut.starter.options.BuildTool;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.SnapStartConf;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.lambda.Alias;
import software.amazon.awscdk.services.lambda.Version;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.route53.*;
import software.constructs.Construct;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.services.apigateway.DomainName;
import software.amazon.awscdk.services.apigateway.EndpointType;
import software.amazon.awscdk.services.apigateway.BasePathMapping;
import software.amazon.awscdk.services.route53.targets.ApiGatewayDomain;

public class AppStack extends Stack {

    public AppStack(final Construct parent, final String id) {
        this(parent, id, null);
    }

    public AppStack(final Construct parent, final String id, final StackProps props) {
        super(parent, id, props);

        Map<String, String> environmentVariables = new HashMap<>();
        Function function = MicronautFunction.create(ApplicationType.DEFAULT,
                false,
                this,
                "micronaut-function")
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
        Version currentVersion = function.getCurrentVersion();
        Alias prodAlias = Alias.Builder.create(this, "ProdAlias")
                .aliasName("Prod")
                .version(currentVersion)
                .build();
        LambdaRestApi api = LambdaRestApi.Builder.create(this, "hello-world-api")
                .handler(prodAlias)
                .build();

        // 配置自定义域名
        String domainName = "api.sunbath.top"; // 替换为你的域名
        String hostedZoneId = "Z1010701L76WABQ482GB"; // 替换为你的 Route 53 托管区域 ID
        String certificateArn = "arn:aws:acm:us-east-1:756850059479:certificate/a42140fb-a401-40bb-9e0d-207182e8c74f"; // 替换为你的 ACM 证书 ARN
        String basePath = "hello-world";

        // 创建 API Gateway 域名
        DomainName apiDomainName = DomainName.Builder.create(this, "ApiDomainName")
                .domainName(domainName)
                .certificate(Certificate.fromCertificateArn(this, "ApiCertificate", certificateArn))
                .endpointType(EndpointType.EDGE)
                .build();

        // 创建路径映射
        BasePathMapping.Builder.create(this, "hello-world")
                .domainName(apiDomainName)
                .basePath("hello-world")
                .restApi(api)
                .build();

        // 创建 Route 53 A 记录
        IHostedZone hostedZone = HostedZone.fromHostedZoneId(this, "HostedZone",
                hostedZoneId);
        ARecord.Builder.create(this, "ApiARecord")
                .zone(hostedZone)
                .recordName(domainName)
                .target(RecordTarget.fromAlias(new ApiGatewayDomain(apiDomainName)))
                .build();

        CfnOutput.Builder.create(this, "MnTestApiUrl")
                .exportName("MnTestApiUrl")
                .value("https://" + domainName + "/" + basePath)
                .build();

        CfnOutput.Builder.create(this, "MnTestApiUrl")
                .exportName("MnTestApiUrl")
                .value(api.getUrl())
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