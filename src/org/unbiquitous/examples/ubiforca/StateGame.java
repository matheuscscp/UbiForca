package org.unbiquitous.examples.ubiforca;

import java.awt.Font;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import org.unbiquitous.ubiengine.game.state.GameStateArgs;
import org.unbiquitous.ubiengine.game.state.GameState;
import org.unbiquitous.ubiengine.resources.input.keyboard.KeyboardDevice;
import org.unbiquitous.ubiengine.resources.input.keyboard.KeyboardDevice.KeyEvent;
import org.unbiquitous.ubiengine.resources.input.keyboard.KeyboardManager;
import org.unbiquitous.ubiengine.resources.video.Screen;
import org.unbiquitous.ubiengine.resources.video.texture.Sprite;
import org.unbiquitous.ubiengine.util.ComponentContainer;
import org.unbiquitous.ubiengine.util.observer.Event;

public final class StateGame extends GameState {
  private Sprite bg;
  private String[] words;
  private String current_word;
  private int current_stage;
  private Sprite[] stages;
  private boolean[] keys;
  private boolean win;
  private Sprite win_sprite;
  private Sprite lose_sprite;
  private Queue<KeyboardDevice> external_keyboards = new LinkedList<KeyboardDevice>();
  
  public StateGame(ComponentContainer components, GameStateArgs args) {
    super(components, args);
  }

  public void init(GameStateArgs args) {
    Screen screen = components.get(Screen.class);
    
    screen.showFPS(true);
    bg = new Sprite(screen, "img/bg.png");
    loadWords();
    loadStages();
    keys = new boolean[26];
    win_sprite = new Sprite(screen, "img/win.png");
    lose_sprite = new Sprite(screen, "img/lose.png");
    
    try {
      components.get(KeyboardManager.class).getMainKeyboard()
      .connect(
        KeyboardDevice.KEYDOWN,
        this,
        StateGame.class.getDeclaredMethod("keyPressed", Event.class)
      );
    } catch (NoSuchMethodException e) {
    } catch (SecurityException e) {
    }
    
    reset();
  }
  
  public void close() {
    
  }
  
  private void loadWords() {
    // loading words
    try {
      List<String> tmplist = Files.readAllLines(Paths.get("words.txt"), StandardCharsets.UTF_8);
      
      // counting not empty strings
      int total = 0;
      Iterator<String> it = tmplist.iterator();
      while (it.hasNext()) {
        String tmpstr = it.next();
        if (tmpstr.length() > 0)
          ++total;
      }
      
      // abort execution if no words found
      if (total == 0)
        throw new Error("Nenhuma palavra encontrada no arquivo de palavras!");
      
      words = new String[total];
      
      // getting strings into the array
      int i = 0;
      it = tmplist.iterator();
      while (it.hasNext()) {
        String tmpstr = it.next();
        if (tmpstr.length() > 0)
          words[i++] = tmpstr.replaceAll("[^A-Z]", "");
      }
    }
    catch (IOException e) {
      throw new Error("Arquivo de palavras \"conf/words.txt\" não encontrado!");
    }
  }
  
  private void loadStages() {
    // load stages sprites
    List<Sprite> tmp = new ArrayList<Sprite>();
    int i = 1;
    while (true) {
      try {
        tmp.add(new Sprite(components.get(Screen.class), String.format("img/%d.png", i++)));
      }
      catch (Error e) {
        break;
      }
    }
    
    // abort execution if only one stage found
    if (i < 4)
      throw new Error("UbiForca não pode ser inicializado com menos do que dois estágios!");
    
    // creating the array
    stages = tmp.toArray(new Sprite[i - 2]);
  }
  
  private void reset() {
    current_word = words[new Random().nextInt(words.length)];
    current_stage = 0;
    for (int i = 0; i < keys.length; ++i)
      keys[i] = false;
    win = false;
  }
  
  public void input() {
    Queue<KeyboardDevice> tmp = new LinkedList<KeyboardDevice>();
    
    while (!external_keyboards.isEmpty()) {
      KeyboardDevice keyboard = external_keyboards.poll();
      if (!keyboard.isPlugged()) {
        tmp.add(keyboard);
        continue;
      }
      
      try {
        keyboard.connect(
          KeyboardDevice.KEYDOWN,
          this,
          StateGame.class.getDeclaredMethod("keyPressed", Event.class)
        );
      } catch (NoSuchMethodException e) {
      } catch (SecurityException e) {
      }
    }
    
    external_keyboards = tmp;
  }

  public void keyPressed(Event event) {
    KeyEvent e = (KeyEvent) event;
    
    // restart game
    if (win || current_stage == stages.length) {
      reset();
      return;
    }
    
    // adjusting char
    int uchar = e.getUnicodeChar();
    if (uchar >= 97 && uchar <= 122)
      uchar -= 32;
    
    // invalid char
    if (uchar < 65 || uchar > 90)
      return;
    
    // key already pressed
    if (keys[uchar - 65])
      return;
    
    keys[uchar - 65] = true;
    if (current_word.indexOf(uchar) == -1)
      ++current_stage;
  }
  
  public void update() {
    
  }
  
  public void render() {
    bg.render();
    
    // if the player won
    if (win)
      win_sprite.render(512, 384, true);
    // show gallows
    else if (current_stage < stages.length) {
      stages[current_stage].render(512, 250, true);
      renderWord();
      renderAlphabet();
    }
    // win lose state
    else
      lose_sprite.render(512, 384, true);
  }
  
  private void renderWord() {
    String tmp = "";
    boolean underline = false;
    for (int i = 0; i < current_word.length(); ++i) {
      if (!keys[(int) (current_word.codePointAt(i) - 'A')]) {
        underline = true;
        tmp += "_ ";
      }
      else
        tmp += (char) current_word.codePointAt(i) + " ";
    }
    if (!underline)
      win = true;
    components.get(Screen.class).renderText(tmp, new Font(Font.MONOSPACED, Font.BOLD, 30), null, 512, 580, true);
  }
  
  private void renderAlphabet() {
    String tmp = "";
    for (char c = 'A'; c <= 'Z'; ++c) {
      if (!keys[(int) (c - 'A')])
        tmp += c + " ";
    }
    components.get(Screen.class).renderText(tmp, new Font(Font.MONOSPACED, Font.BOLD, 22), null, 512, 680, true);
  }
  
  protected void handleNewKeyboardDevice(KeyboardDevice keyboard_device) {
    components.get(KeyboardManager.class).sendRequest(keyboard_device);
    external_keyboards.add(keyboard_device);
  }
}
