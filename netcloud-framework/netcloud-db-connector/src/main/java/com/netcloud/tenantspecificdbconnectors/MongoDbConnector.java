package com.netcloud.tenantspecificdbconnectors;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.netcloud.dtos.DbMetadata;
import lombok.NonNull;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import javax.management.MalformedObjectNameException;
import java.util.Map;

public class MongoDbConnector {
    private final DbMetadata dbMetadata;

    public MongoDbConnector(@NonNull DbMetadata metadata)
    {
        this.dbMetadata = metadata;
    }
    public MongoDatabase getMongoDbInstance() throws MalformedObjectNameException {
        MongoDatabase database = null;
        try (MongoClient mongoClient = MongoClients.create(dbMetadata.getConnectionString())) {

            database = mongoClient.getDatabase(dbMetadata.getDbName());
            System.out.println("Connected to database: " + dbMetadata.getDbName());

        } catch (Exception e) {
            throw new MalformedObjectNameException(String.format("Error connecting with database %s", database));
        }
        return database;
    }
    private MongoCollection<Document> getMongoCollection(String collectionName, MongoDatabase database) throws MalformedObjectNameException {
        return database.getCollection(collectionName);
    }
    public <V> void insertDocument(Map<String, V> values, String collectionName, MongoDatabase database) throws MalformedObjectNameException {
        MongoCollection<Document> collection = getMongoCollection(collectionName, database);
        Document document = new Document();
        for(String key : values.keySet())
        {
            document.append(key, values.get(key));
        }
        collection.insertOne(document);
    }
    public <V> boolean updateDocument(Map<String, V> values, String collectionName, MongoDatabase database) throws MalformedObjectNameException {
        MongoCollection<Document> collection = getMongoCollection(collectionName, database);

        Bson bson = new BsonDocument();
        for(String key : values.keySet())
        {
            bson = Updates.set(key, values.get(key));
        }
        collection.findOneAndUpdate(
                Filters.eq("id", values.get("id")),
                bson
                );
        return true;
    }
    public <V> Document deleteDocument(Map<String, V> values, String collectionName, MongoDatabase database) throws MalformedObjectNameException {
        MongoCollection<Document> collection = getMongoCollection(collectionName, database);

        return collection.findOneAndDelete(
                Filters.eq("id", values.get("id"))
        );
    }
}
