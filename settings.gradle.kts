rootProject.name = "helloworld"

include("apps:backend:helloworld:app")
include("apps:backend:helloworld:infra")
include("apps:backend:auth:app")
include("apps:backend:auth:infra")
include("apps:backend:email:app")
include("apps:backend:email:infra")

include("libs:jvm-shared-lib")
