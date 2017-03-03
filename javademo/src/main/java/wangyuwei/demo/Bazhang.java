package wangyuwei.demo;

import wangyuwei.costtime.Cost;

/**
 * 巴掌
 * https://github.com/JeasonWong
 */

public class Bazhang {

  public void newFunc1(String str) {
    System.out.println(str);
    for (int i = 0; i < 100; i++) {
      if (i % 10 == 0) {
        System.out.println(i);
      }
      if (i == 50) {
        return;
      }
    }
  }

  @Cost
  public void newFunc2(String str) {
    System.out.println(str);
    for (int i = 0; i < 100; i++) {
      if (i % 8 == 0) {
        System.out.println(i);
      }
      if (i > 50) {
        return;
      }
    }
  }

}
