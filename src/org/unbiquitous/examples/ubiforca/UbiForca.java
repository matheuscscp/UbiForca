package org.unbiquitous.examples.ubiforca;

import java.util.HashMap;

import org.unbiquitous.ubiengine.game.UosGame;

public final class UbiForca extends UosGame {
  public HashMap<String, Object> getSettings() {
    HashMap<String, Object> prop = new HashMap<String, Object>();
    
    prop.put("window_title", "UbiForca");
    prop.put("window_width", 1024);
    prop.put("window_height", 768);
    
    prop.put("first_state", StateGame.class.getName());
    
    return prop;
  }
}
