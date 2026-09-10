package com.custody.core.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JsonValues {
  private final ObjectMapper mapper;

  public JsonValues(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public String write(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (Exception exception) {
      throw new IllegalStateException("Cannot encode internal value", exception);
    }
  }

  public Map<String, String> read(String value) {
    try {
      return mapper.readValue(value, new TypeReference<Map<String, String>>() {});
    } catch (Exception exception) {
      throw new IllegalStateException("Cannot decode stored value", exception);
    }
  }
}
