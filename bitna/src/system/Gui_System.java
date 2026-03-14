package system;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;

public class Gui_System {
  public static long getUsedMemoryMB() {
    return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024L / 1024L;
  }
  
  public static long getTotalMemoryMB() {
    return Runtime.getRuntime().maxMemory() / 1024L / 1024L;
  }
  
  public static long getMemoryMB() {
    return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024L / 1024L;
  }
  
  public static long getToTalMemoryMB() {
    return Runtime.getRuntime().totalMemory() / 1024L / 1024L;
  }
  
  public static long getFreeMemoryMB() {
    return Runtime.getRuntime().freeMemory() / 1024L / 1024L;
  }
  
  public static double getUseCpu() {
    try {
      Object operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
      Method method = operatingSystemMXBean.getClass().getMethod("getSystemCpuLoad", new Class[0]);
      Object value = method.invoke(operatingSystemMXBean, new Object[0]);
      if (value instanceof Number) {
        double cpu = ((Number)value).doubleValue();
        if (cpu >= 0.0D)
          return cpu * 100.0D; 
      } 
    } catch (Exception exception) {}
    return 0.0D;
  }
  
  public static int getThread() {
    return Thread.activeCount();
  }
}
