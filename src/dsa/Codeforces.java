import java.util.*;
import java.io.*;
public class Codeforces {
    public static void main(String args[]){
        InputReader in = new InputReader();
        PrintWriter out = new PrintWriter(System.out);
        Task solver = new Task();
        solver.solve(1, in, out);
        out.close();
    }
    static class Pair{
        int x;
        int y;
        Pair(int x,int y){
            this.x = x;
            this.y = y;
        }
    }
    static class Task {

        public void solve(int t, InputReader in, PrintWriter out) {
            int n = in.nextInt(), k = in.nextInt();
            int[] a = new int[n];

            for (int i = 0; i < n; i++) {
                a[i] = in.nextInt();
            }

            // Every cowbell can have its own box
            if (n <= k) {
                out.println(a[n - 1]);
                return;
            }

            int pairs = n - k;
            int ans = 0;

            // Pair smallest with largest
            for (int i = 0; i < pairs; i++) {
                ans = Math.max(ans, a[i] + a[2*pairs -1 - i]);
            }

            // Remaining cowbells go alone
            for (int i = 2 * pairs; i < n; i++) {
                ans = Math.max(ans, a[i]);
            }
            out.println(ans);

            out.close();
        }
    }
        static class InputReader {
            BufferedReader br;
            StringTokenizer st;
            public InputReader() {
                br = new BufferedReader(new
                        InputStreamReader(System.in));
            }
            String next() {
                while (st == null || !st.hasMoreElements()) {
                    try {
                        st = new StringTokenizer(br.readLine());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return st.nextToken();
            }
            int nextInt() {
                return Integer.parseInt(next());
            }
            long nextLong() {
                return Long.parseLong(next());
            }
            double nextDouble() {
                return Double.parseDouble(next());
            }
            String nextLine() {
                String str = "";
                try {
                    str = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return str;
            }
        }
}