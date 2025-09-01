import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    // Flip to true to see debug info
    static final boolean VERBOSE = false;

    public static void main(String[] args) throws Exception {
        String json = readAll(System.in);

        // 1) Extract k (so m = k-1)
        int k = extractInt(json, "\"k\"\\s*:\\s*(\\d+)");
        int m = k - 1;
        if (m < 0) {
            System.out.println(); // nothing to do
            return;
        }

        // 2) Extract all numbered roots: "1": { "base": "...", "value": "..." }
        Pattern p = Pattern.compile(
                "\"(\\d+)\"\\s*:\\s*\\{\\s*\"base\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"value\"\\s*:\\s*\"([^\"]+)\"\\s*\\}");
        Matcher matcher = p.matcher(json);

        List<Root> roots = new ArrayList<>();
        while (matcher.find()) {
            int label = Integer.parseInt(matcher.group(1));
            int base = Integer.parseInt(matcher.group(2).trim());
            String valStr = matcher.group(3).trim();
            BigInteger value = parseInBase(valStr, base);
            roots.add(new Root(label, value));
        }

        // Sort by numeric key: "1","2","3","6", ...
        roots.sort(Comparator.comparingInt(r -> r.label));

        if (roots.size() < m) {
            throw new IllegalArgumentException("Not enough roots provided to build degree " + m + " polynomial.");
        }

        // 3) Take first m roots and build monic polynomial via convolution by (x - r)
        BigInteger[] coeffs = new BigInteger[] { BigInteger.ONE }; // start with P(x) = 1
        for (int i = 0; i < m; i++) {
            coeffs = multiplyByMonicLinear(coeffs, roots.get(i).value);
            if (VERBOSE) System.err.println("After root " + roots.get(i).value + ": " + Arrays.toString(coeffs));
        }

        // coeffs are a0..am (constant to leading). Print am..a0
        StringBuilder out = new StringBuilder();
        for (int i = coeffs.length - 1; i >= 0; i--) {
            if (out.length() > 0) out.append(' ');
            out.append(coeffs[i].toString());
        }
        System.out.println(out.toString());
    }

    // Multiply a polynomial (given as a0..ad) by (x - r), return new coeffs a0'..a(d+1)'
    private static BigInteger[] multiplyByMonicLinear(BigInteger[] a, BigInteger r) {
        BigInteger[] b = new BigInteger[a.length + 1];
        Arrays.fill(b, BigInteger.ZERO);
        // (x) * P(x): shift
        for (int i = 0; i < a.length; i++) {
            b[i + 1] = b[i + 1].add(a[i]);
        }
        // (-r) * P(x): subtract r*a[i] into same degree
        for (int i = 0; i < a.length; i++) {
            b[i] = b[i].subtract(r.multiply(a[i]));
        }
        return b;
    }

    // Parse number string in base 2..36 into BigInteger (handles optional leading '-')
    private static BigInteger parseInBase(String s, int base) {
        if (base < 2 || base > 36) throw new IllegalArgumentException("Base out of range: " + base);
        s = s.trim();
        boolean neg = false;
        int idx = 0;
        if (s.startsWith("-")) { neg = true; idx = 1; }
        BigInteger res = BigInteger.ZERO;
        BigInteger B = BigInteger.valueOf(base);
        for (; idx < s.length(); idx++) {
            char c = s.charAt(idx);
            int v = digitVal(c);
            if (v < 0 || v >= base) throw new IllegalArgumentException("Invalid digit '" + c + "' for base " + base);
            res = res.multiply(B).add(BigInteger.valueOf(v));
        }
        return neg ? res.negate() : res;
    }

    private static int digitVal(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'z') return 10 + (c - 'a');
        if (c >= 'A' && c <= 'Z') return 10 + (c - 'A');
        return -1;
    }

    private static int extractInt(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        if (!m.find()) throw new IllegalArgumentException("Missing int for pattern: " + regex);
        return Integer.parseInt(m.group(1));
    }

    private static String readAll(InputStream in) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        }
    }

    private static class Root {
        int label;
        BigInteger value;
        Root(int label, BigInteger value) { this.label = label; this.value = value; }
    }
}