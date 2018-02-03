package org.jchien.shuffle.model;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author jchien
 */
public class UserRunDetailsTestUtils {
    public static List<UserRunDetails> generateUserRunDetails(int numRuns, int minCharsPerRow) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < minCharsPerRow; i++) {
            sb.append("a");
        }

        Pokemon pokemon = new Pokemon(sb.toString(),
                                      null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      false);

        RunDetails run = new RunDetailsBuilder()
                .setTeam(Arrays.asList(pokemon))
                .build();

        List<UserRunDetails> ret = new ArrayList<>(numRuns);

        DecimalFormat df = new DecimalFormat("000000");
        for (int i=0; i < numRuns; i++) {
            String user = "run-" + df.format(i);
            UserRunDetails urd = new UserRunDetails(user, "abc", run);
            ret.add(urd);
        }
        return ret;
    }
}
