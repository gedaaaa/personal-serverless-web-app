package top.sunbath.api.auth;

import io.micronaut.aws.cdk.function.MicronautFunction;
import io.micronaut.aws.cdk.function.MicronautFunctionFile;
import io.micronaut.starter.application.ApplicationType;
import io.micronaut.starter.options.BuildTool;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;
import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awscdk.services.lambda.CfnFunction;
import software.amazon.awscdk.services.iam.ServicePrincipal;

public class AppStack extends Stack {

        public AppStack(final Construct parent, final String id) {
                this(parent, id, null);
        }

        public AppStack(final Construct parent, final String id, final StackProps props) {
                super(parent, id, props);

                var serviceName = "email";
                var dynamodbSingleTableName = serviceName + "-service-single-table";
                var dynamodbDistributedLocksTableName = "distributed_locks";

                // Create Dead Letter Queue
                var emailQueueDlq = Queue.Builder.create(this, serviceName + "-dlq").queueName(serviceName + "-dlq")
                                .retentionPeriod(Duration.days(14)).build();

                // Create main queue with DLQ
                var emailQueue = Queue.Builder.create(this, serviceName + "-queue").queueName(serviceName + "-queue")
                                .retentionPeriod(Duration.days(14))
                                // according to SQS docs, the visibility timeout should be at least 6 times
                                // the maximum timeout of the lambda function
                                .visibilityTimeout(Duration.seconds(30 * 6)).deadLetterQueue(DeadLetterQueue.builder()
                                                .maxReceiveCount(3).queue(emailQueueDlq).build())
                                .build();

                // Create Dead Letter Queue
                var preventEmailJobQueueDlq = Queue.Builder.create(this, serviceName + "-cancel-dlq")
                                .queueName("prevent-email-job-dlq").retentionPeriod(Duration.days(14)).build();

                // Create prevent email job queue with DLQ
                var preventEmailJobQueue = Queue.Builder.create(this, serviceName + "-cancel-queue")
                                .queueName("prevent-email-job-queue").retentionPeriod(Duration.days(14))
                                // according to SQS docs, the visibility timeout should be at least 6 times
                                // the maximum timeout of the lambda function
                                .visibilityTimeout(Duration.seconds(30 * 6)).deadLetterQueue(DeadLetterQueue.builder()
                                                .maxReceiveCount(3).queue(preventEmailJobQueueDlq).build())
                                .build();

                Map<String, String> environmentVariables = new HashMap<>();
                // 设置生产环境
                environmentVariables.put("MICRONAUT_ENVIRONMENTS", "production");
                // 设置 DynamoDB 表名
                environmentVariables.put("DYNAMODB_TABLE_NAME", dynamodbSingleTableName);
                // 设置 SQS 队列 URL
                environmentVariables.put("SQS_QUEUE_URL", emailQueue.getQueueUrl());

                var emailFunction = MicronautFunction
                                .create(ApplicationType.DEFAULT, false, this, serviceName + "-function")
                                .runtime(Runtime.JAVA_21).handler("top.sunbath.api.email.handler.EmailFunctionHandler")
                                .environment(environmentVariables).code(Code.fromAsset(functionPath()))
                                .timeout(Duration.seconds(30)).memorySize(512).logRetention(RetentionDays.ONE_WEEK)
                                .tracing(Tracing.ACTIVE).architecture(Architecture.X86_64)
                                .snapStart(SnapStartConf.ON_PUBLISHED_VERSIONS).build();

                var cancelEmailJobFunction = MicronautFunction
                                .create(ApplicationType.DEFAULT, false, this, serviceName + "-cancel-function")
                                .runtime(Runtime.JAVA_21)
                                .handler("top.sunbath.api.email.handler.CancelEmailFunctionHandler")
                                .environment(environmentVariables).code(Code.fromAsset(functionPath()))
                                .timeout(Duration.seconds(30)).memorySize(512).logRetention(RetentionDays.ONE_WEEK)
                                .build();

                // Set reserved concurrent executions to 1
                CfnFunction cfnFunction = (CfnFunction) emailFunction.getNode().getDefaultChild();
                cfnFunction.setReservedConcurrentExecutions(1);

                // Grant SQS permissions to Lambda
                emailQueue.grantConsumeMessages(emailFunction);
                // Grant DLQ send permissions to queue
                PolicyStatement dlqAccessPolicy = PolicyStatement.Builder.create()
                                .actions(Arrays.asList("sqs:SendMessage"))
                                .resources(Arrays.asList(emailQueueDlq.getQueueArn()))
                                .principals(Arrays.asList(new ServicePrincipal("lambda.amazonaws.com")))
                                .conditions(Map.of("ArnEquals", Map.of("aws:SourceArn", emailQueue.getQueueArn())))
                                .build();
                emailQueueDlq.addToResourcePolicy(dlqAccessPolicy);

                PolicyStatement preventDlqAccessPolicy = PolicyStatement.Builder.create()
                                .actions(Arrays.asList("sqs:SendMessage"))
                                .resources(Arrays.asList(preventEmailJobQueueDlq.getQueueArn()))
                                .principals(Arrays.asList(new ServicePrincipal("lambda.amazonaws.com")))
                                .conditions(Map.of("ArnEquals",
                                                Map.of("aws:SourceArn", preventEmailJobQueue.getQueueArn())))
                                .build();
                preventEmailJobQueueDlq.addToResourcePolicy(preventDlqAccessPolicy);

                // Output queue information
                CfnOutput.Builder.create(this, "QueueUrl").exportName(serviceName + "-queue-url")
                                .value(emailQueue.getQueueUrl()).build();

                CfnOutput.Builder.create(this, "DlqUrl").exportName(serviceName + "-dlq-url")
                                .value(emailQueueDlq.getQueueUrl()).build();

                // 获取当前区域和账户 ID
                String region = this.getRegion();
                String accountId = this.getAccount();

                // 构建分布式锁表的 ARN（包含账户 ID）
                String distributedLocksArn = String.format("arn:aws:dynamodb:%s:%s:table/%s", region, accountId,
                                dynamodbDistributedLocksTableName);
                var distributedLocksTable = Table.fromTableArn(this, "DistributedLocksTable", distributedLocksArn);

                String dynamodbSingleTableArn = String.format("arn:aws:dynamodb:%s:%s:table/%s", region, accountId,
                                dynamodbSingleTableName);
                var singleTable = Table.fromTableArn(this, "SingleTable", dynamodbSingleTableArn);

                // 授予 Lambda 函数对 DynamoDB 表的读写权限
                singleTable.grantReadWriteData(emailFunction);
                distributedLocksTable.grantReadWriteData(emailFunction);
                singleTable.grantReadWriteData(cancelEmailJobFunction);
                distributedLocksTable.grantReadWriteData(cancelEmailJobFunction);

                // 额外授予 Lambda 函数创建和管理索引的权限
                emailFunction.addToRolePolicy(PolicyStatement.Builder.create().effect(Effect.ALLOW)
                                .actions(Arrays.asList("dynamodb:UpdateTable", "dynamodb:DescribeTable",
                                                "dynamodb:CreateTable"))
                                .resources(Arrays.asList(singleTable.getTableArn(),
                                                // index
                                                singleTable.getTableArn() + "/*", distributedLocksArn))
                                .build());
                cancelEmailJobFunction.addToRolePolicy(PolicyStatement.Builder.create().effect(Effect.ALLOW)
                                .actions(Arrays.asList("dynamodb:UpdateTable", "dynamodb:DescribeTable",
                                                "dynamodb:CreateTable"))
                                .resources(Arrays.asList(singleTable.getTableArn(),
                                                // index
                                                singleTable.getTableArn() + "/*", distributedLocksArn))
                                .build());
                emailFunction.addToRolePolicy(PolicyStatement.Builder.create().effect(Effect.ALLOW)
                                .actions(Arrays.asList("dynamodb:Query", "dynamodb:Scan", "dynamodb:GetItem",
                                                "dynamodb:PutItem", "dynamodb:UpdateItem", "dynamodb:DeleteItem"))
                                .resources(Arrays.asList(singleTable.getTableArn(),
                                                // index
                                                singleTable.getTableArn() + "/*", distributedLocksArn))
                                .build());
                cancelEmailJobFunction.addToRolePolicy(PolicyStatement.Builder.create().effect(Effect.ALLOW)
                                .actions(Arrays.asList("dynamodb:Query", "dynamodb:Scan", "dynamodb:GetItem",
                                                "dynamodb:PutItem", "dynamodb:UpdateItem", "dynamodb:DeleteItem"))
                                .resources(Arrays.asList(singleTable.getTableArn(),
                                                // index
                                                singleTable.getTableArn() + "/*", distributedLocksArn))
                                .build());

                String ssmParameterArn = String.format("arn:aws:ssm:%s:%s:parameter/auth/resend/api-key", region,
                                accountId);

                // 添加 SSM Parameter Store 访问权限
                emailFunction.addToRolePolicy(PolicyStatement.Builder.create().effect(Effect.ALLOW)
                                .actions(Arrays.asList("ssm:GetParameter")).resources(Arrays.asList(ssmParameterArn))
                                .build());
                cancelEmailJobFunction.addToRolePolicy(PolicyStatement.Builder.create().effect(Effect.ALLOW)
                                .actions(Arrays.asList("ssm:GetParameter")).resources(Arrays.asList(ssmParameterArn))
                                .build());

                var currentEmailFunctionVersion = emailFunction.getCurrentVersion();
                var emailFunctionProdAlias = Alias.Builder.create(this, "ProdAlias").aliasName("Prod")
                                .version(currentEmailFunctionVersion).build();
                var currentCancelEmailJobFunctionVersion = cancelEmailJobFunction.getCurrentVersion();
                var cancelEmailJobFunctionProdAlias = Alias.Builder.create(this, "CancelEmailJobProdAlias")
                                .aliasName("Prod").version(currentCancelEmailJobFunctionVersion).build();

                // Create SQS event source mapping
                SqsEventSource emailEventSource = SqsEventSource.Builder.create(emailQueue)
                                // Process one message at a time due to rate limit
                                .batchSize(1).build();
                emailFunctionProdAlias.addEventSource(emailEventSource);

                SqsEventSource cancelEmailEventSource = SqsEventSource.Builder.create(preventEmailJobQueue).batchSize(1)
                                .build();
                cancelEmailJobFunctionProdAlias.addEventSource(cancelEmailEventSource);

                // 输出 DynamoDB 表名
                CfnOutput.Builder.create(this, "SingleTableName").exportName(serviceName + "-SingleTableName")
                                .value(singleTable.getTableName()).build();
        }

        public static String functionPath() {
                return "../app/build/libs/" + functionFilename();
        }

        public static String functionFilename() {
                return MicronautFunctionFile.builder().optimized().graalVMNative(false).version("0.1")
                                .archiveBaseName("app").buildTool(BuildTool.GRADLE).build();
        }
}