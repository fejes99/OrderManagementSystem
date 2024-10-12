package se.david.api.event;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;

import java.time.ZonedDateTime;
import java.util.List;

import static java.time.ZonedDateTime.now;

public class Event<K, T> {

  public enum Type {
    CREATE,
    UPDATE,

    INCREASE_STOCK,
    REDUCE_STOCKS
  }

  private final Type eventType;
  private final K key;
  private final T data;
  private final List<T> dataList;
  private final ZonedDateTime eventCreatedAt;

  public Event() {
    this.eventType = null;
    this.key = null;
    this.data = null;
    this.dataList = null;
    this.eventCreatedAt = null;
  }

  public Event(Type eventType, K key, T data) {
    this.eventType = eventType;
    this.key = key;
    this.data = data;
    this.dataList = null;   // Only data is used
    this.eventCreatedAt = now();
  }

  public Event(Type eventType, K key, List<T> dataList) {
    this.eventType = eventType;
    this.key = key;
    this.data = null;
    this.dataList = dataList;
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

  public List<T> getDataList() {
    return dataList;
  }

  @JsonSerialize(using = ZonedDateTimeSerializer.class)
  public ZonedDateTime getEventCreatedAt() {
    return eventCreatedAt;
  }

  public boolean isDataList() {
    return dataList != null;
  }

  public boolean isSingleData() {
    return data != null;
  }

}

