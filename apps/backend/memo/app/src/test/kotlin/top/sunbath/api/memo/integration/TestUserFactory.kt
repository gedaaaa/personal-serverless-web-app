package top.sunbath.api.memo.integration

import top.sunbath.shared.types.CurrentUser
import java.util.UUID

object TestUserFactory {
    fun createUserInfo(
        id: String = UUID.randomUUID().toString(),
        email: String = "test@example.com",
        username: String = "testuser",
    ): CurrentUser =
        CurrentUser(
            id = id,
            email = email,
            username = username,
        )
}
