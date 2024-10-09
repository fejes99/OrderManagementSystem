package se.david.api.event;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;

import java.time.ZonedDateTime;

import static java.time.ZonedDateTime.now;

public class Event<K, T> {

  public enum Type {
    CREATE,       // For creation of entities (POST)
    DELETE,       // For deletion of entities (DELETE)
    UPDATE,       // For updating entities (PUT)
    CONFIRMATION, // For confirmation-related events (e.g., shipping confirmation)
    RESERVE,      // For reserving resources (e.g., inventory reservation)
    RELEASE,      // For releasing reserved resources (e.g., inventory release after failed order)
    REJECT        // For rejecting requests or failed operations
  }

  private final Type eventType;
  private final K key;
  private final T data;
  private final ZonedDateTime eventCreatedAt;

  public Event() {
    this.eventType = null;
    this.key = null;
    this.data = null;
    this.eventCreatedAt = null;
  }

  public Event(Type eventType, K key, T data) {
    this.eventType = eventType;
    this.key = key;
    this.data = data;
    this.eventCreatedAt = now();
  }

  public Type getEventType() {
    return eventType;
  }

  public K getKey() {
    return key;
  }

  public T getData() {
    return data;
  }

  @JsonSerialize(using = ZonedDateTimeSerializer.class)
  public ZonedDateTime getEventCreatedAt() {
    return eventCreatedAt;
  }
}

