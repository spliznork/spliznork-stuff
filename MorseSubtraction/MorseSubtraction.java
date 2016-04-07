import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class MorseSubtraction {

  private static final Map<String, String> DEFAULT_MESSAGES = buildDefaultMessages();
  private static final String VALIDATE_MESSAGE_PATTERN = "[*_-]+";

  private static Map<String, String> buildDefaultMessages() {
    Map<String, String> map = new HashMap<>();
    map.put("AB", "*-_-***");
    map.put("R", "*-*");
    map.put("HELLO", "****_*_*-**_*-**_---___*--_---_*-*_*-**_-**");
    map.put("HELP", "****_*_*-**_*--*");
    map.put("ABCD", "*-_-***_-*-*_-**");
    map.put("ST", "***_-");
    map.put("ZN", "--**_-*");
    map.put("STARWARS", "-_****_*___***_-_*-_*-*___*--_*-_*-*_***___***_*-_--*_*-");
    map.put("YODA", "-*--_---_-**_*-");
    map.put("LEIA", "*-**_*_**_*-");
    return Collections.unmodifiableMap(map);
  }

  public static void main(String... args) {
    if (args.length < 2) {
      System.out.println("Usage: MorseSubtraction [message_a] [message_b] (message_c) (...)");
      return;
    }

    List<String> messages = new ArrayList<>(args.length);
    for (String arg : args) {
      messages.add(decodeMessage(arg));
    }

    long t0 = System.nanoTime();
    Set<String> results = bruteForceSequence(messages);
    long t1 = System.nanoTime();

    for (String result : new TreeSet<String>(results)) {
      System.out.println(result);
    }
    System.out.format("%d solutions\n", results.size());
    System.out.format("%.3f seconds\n", (t1 - t0) / 1e9);

  }

  private static String decodeMessage(String id) {
    if (DEFAULT_MESSAGES.containsKey(id)) {
      return DEFAULT_MESSAGES.get(id);
    }
    if (id.matches(VALIDATE_MESSAGE_PATTERN)) {
      return id;
    }
    throw new IllegalArgumentException("Unknown or malformed message " + id);
  }

  public static Set<String> bruteForceSequence(List<String> messages) {
    Set<String> results = Collections.singleton(messages.get(0));
    for (int i = 1; i < messages.size(); i++) {
      String b = messages.get(i);
      Set<String> newResults = new HashSet<>();
      for (String a : results) {
        newResults.addAll(new MorseSubtraction(a, b).bruteForce());
      }
      results = newResults;
    }
    return results;
  }

  private final String haystack;
  private final String needle;

  public MorseSubtraction(String haystack, String needle) {
    this.haystack = haystack;
    this.needle = needle;
  }

  public Set<String> bruteForce() {
    Set<String> results = new HashSet<>();
    searchByScanning("", haystack, needle, results);
    results.remove("");
    return results;
  }

  private void searchByScanning(
      String partial,
      String remainingHaystack,
      String remainingNeedle,
      Set<String> results) {

    if (remainingNeedle.length() == 0) {
      results.add(partial + remainingHaystack);
      return;
    }

    if (remainingNeedle.length() >= remainingHaystack.length()) {
      if (remainingNeedle.equals(remainingHaystack)) {
        results.add(partial);
      }
      return;
    }

    int i = -1;
    while (true) {
      i = remainingHaystack.indexOf(remainingNeedle.charAt(0), i + 1);
      if ((i < 0) || ((remainingHaystack.length() - i) < remainingNeedle.length())) {
        return;
      }

      searchByScanning(
          partial + remainingHaystack.substring(0, i),
          remainingHaystack.substring(i + 1),
          remainingNeedle.substring(1), 
          results);
    }
  }
}

