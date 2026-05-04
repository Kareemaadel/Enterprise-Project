/*
 * TL;DR on why this file exists:
 * Hibernate 6's multi-tenancy is picky and demands we use a String for the 
 * tenant ID in Java. But we use real UUIDs in the database like civilized people.
 *
 * This converter is the middleman. It lies to Hibernate, telling it we're using 
 * Strings so the app actually boots up, but swaps them out for real UUIDs under 
 * the hood before touching the DB. Keeps the database constraints from yelling at us
 * and lets us leverage the faster times of UUID indexing
 */
package com.example.WorkHub.tenant;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.UUID;

@Converter
public class UuidStringConverter implements AttributeConverter<String, UUID> {

    @Override
    public UUID convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return UUID.fromString(attribute);
        } catch (IllegalArgumentException e) {
            // Handle cases where the sentinel is not a valid UUID format
            // though we are now using a valid zero-UUID sentinel.
            return null;
        }
    }

    @Override
    public String convertToEntityAttribute(UUID dbData) {
        return dbData == null ? null : dbData.toString();
    }
}
