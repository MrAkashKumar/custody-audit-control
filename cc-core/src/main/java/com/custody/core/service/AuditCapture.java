package com.custody.core.service;

/** Records a registered business event in the caller's existing transaction. */
public interface AuditCapture {
  <T> void record(T event);
}
