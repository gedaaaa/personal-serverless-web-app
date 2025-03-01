package top.sunbath.api.auth.dynamodbUtil

/**
 * Defines a DynamoDB index with its name and attribute names.
 */
data class IndexDefinition(
    /**
     * The name of the index.
     */
    val indexName: String,
    /**
     * The name of the partition key attribute.
     */
    val partitionKeyName: String,
    /**
     * The name of the sort key attribute.
     */
    val sortKeyName: String,
)

/**
 * Interface for entities that need custom indexes.
 */
interface Indexable {
    /**
     * Returns a map of index values for this entity.
     * The key is the attribute name, and the value is the attribute value.
     */
    fun getIndexValues(): Map<String, String>
}
