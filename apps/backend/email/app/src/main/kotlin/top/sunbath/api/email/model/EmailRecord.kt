package top.sunbath.api.email.model

import io.micronaut.core.annotation.Creator
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.serde.annotation.Serdeable
import top.sunbath.shared.dynamodb.Identified
import top.sunbath.shared.dynamodb.Indexable

/**
 * A User entity.
 */
@Introspected
@Serdeable
class EmailRecord :
    Identified,
    Indexable {
    @get:NonNull
    override var id: String = ""

    @get:NonNull
    var to: String = ""

    @get:NonNull
    var from: String = ""

    @get:NonNull
    var subject: String = ""

    @get:NonNull
    var html: String = ""

    @get:Nullable
    var vendorResponse: String = ""

    /**
     * Default constructor.
     */
    constructor()

    /**
     * Constructor with all properties.
     * @param id The user ID
     * @param to The email to
     * @param from The email from
     * @param subject The email subject
     * @param html The email html
     * @param vendorResponse The email vendor response
     */
    @Creator
    constructor(
        id: String,
        to: String,
        from: String,
        subject: String,
        html: String,
        vendorResponse: String,
    ) {
        this.id = id
        this.to = to
        this.from = from
        this.subject = subject
        this.html = html
        this.vendorResponse = vendorResponse
    }

    /**
     * Returns index values for this user.
     */
    override fun getIndexValues(): Map<String, String> {
        val indexValues = mutableMapOf<String, String>()

        // Add username index values - use the same constants as in DefaultEmailRecordRepository
        to?.let {
            indexValues["TO_PK"] = it
            indexValues["TO_SK"] = id
        }

        return indexValues
    }
}
