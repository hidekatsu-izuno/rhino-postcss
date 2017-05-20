package net.arnx.rhinode.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolManager{
  private static InheritableThreadLocal<ExecutorService> local = new InheritableThreadLocal<ExecutorService> () {
    @Override
    protected ExecutorService initialValue() {
      return Executors.newCachedThreadPool();
    }
  };

  public static synchronized ExecutorService get() {
    return local.get();
  }

  public static void clear() {
    local.remove();
  }
}
