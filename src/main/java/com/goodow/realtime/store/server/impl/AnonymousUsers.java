/*
 * Copyright 2014 Goodow.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.goodow.realtime.store.server.impl;

import com.google.inject.Inject;

import com.goodow.realtime.channel.util.IdGenerator;

import java.util.Random;

public class AnonymousUsers {
  private static final String ANONYMOUS = "Anonymous ";
  private static final String[] animals = {
      "Alligator", "Anteater", "Armadillo", "Auroch", "Axolotl", "Badger", "Bat", "Beaver",
      "Buffalo", "Camel", "Chameleon", "Cheetah", "Chinchilla", "Chipmunk", "Chupacabra",
      "Cormorant", "Coyote", "Crow", "Dingo", "Dinosaur", "Dolphin", "Duck", "Elephant", "Ferret",
      "Fox", "Frog", "Giraffe", "Gopher", "Grizzly", "Hedgehog", "Hippo", "Hyena", "Ibex", "Ifrit",
      "Iguana", "Jackal", "Jackalope", "Koala", "Kraken", "Lemur", "Leopard", "Liger", "Llama",
      "Manatee", "Mink", "Monkey", "Narwhal", "Nyan Cat", "Orangutan", "Otter", "Panda", "Penguin",
      "Platypus", "Pumpkin", "Python", "Quagga", "Rabbit", "Raccoon", "Rhino", "Sheep", "Shrew",
      "Skunk", "Slow loris", "Squirrel", "Turtle", "Walrus", "Wolf", "Wolverine", "Wombat"};
  private static final String[] colors = {
      "#5484ed", "#a4bdfc", "#46d6db", "#7ae7bf", "#51b749", "#fbd75b", "#ffb878", "#ff887c",
      "#dc2127", "#dbadff"};
  private static final String[] colors2 = {
      "#a61c00", "#cc0000", "#e69138", "#f1c232", "#6aa84f", "#45818e", "#3c78d8", "#3d85c6",
      "#674ea7", "#a64d79"};
  @Inject private Random random;
  @Inject private IdGenerator idGenerator;

  public String getColor() {
    return colors[random.nextInt(colors.length - 1)];
  }

  public String getDisplyName() {
    return ANONYMOUS + animals[random.nextInt(animals.length - 1)];
  }

  public String getPhotoUrl(String displyName) {
    return "https://ssl.gstatic.com/docs/common/profile/"
        + displyName.substring(ANONYMOUS.length()).toLowerCase().replaceAll(" ", "") + "_lg.png";
  }

  public String getUserId() {
    return "ANONYMOUS_" + idGenerator.nextNumbers(21);
  }
}
