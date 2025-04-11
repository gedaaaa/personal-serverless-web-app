package top.sunbath.shared.utils

fun isRunningInAwsLambda(): Boolean =
    System.getenv("AWS_LAMBDA_FUNCTION_NAME") != null ||
        System.getenv("AWS_LAMBDA_RUNTIME_API") != null
