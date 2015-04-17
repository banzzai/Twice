package interview.twice.com.twice;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

public class MainActivity extends Activity {
    private EditText mUserInput;
    private TextView mDisplay;
    private TextView mResults;

    private static HashSet<String> mDictionary = new HashSet<String>();
    private boolean mDictionaryReady = false;

    // As explained below, taking small shortcuts with how I'm treating the dictionary as I want to
    // focus on the solving part of the exercise. Loading the dictionary could use some more work.
    private static final int WORDS_IN_LANGUAGE = 172820;

    // Set of the solutions found
    private static HashSet<String> mSolutions = new HashSet<String>();

    static void permute(int level, String permuted,
                        boolean used[], String original) {
        int length = original.length();
        if (level == length) {
            System.out.println(permuted);
        } else {
            for (int i = 0; i < length; i++) {
                if (!used[i]) {
                    used[i] = true;
                    permute(level + 1, permuted + original.charAt(i),
                            used, original);
                    used[i] = false;
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUserInput = (EditText) findViewById(R.id.textInput);
        mDisplay = (TextView) findViewById(R.id.display);
        mResults = (TextView) findViewById(R.id.results);

        initDictionary();
        display(getString(R.string.loading));
    }

    // Show a message on screen
    private void display(final String string)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDisplay.setText(string);
            }
        });
    }

    // My understanding is that loading the dictionary wasn't quite the point of the exercise.
    // With more time I would more likely look for a more efficient method, perhaps look for a file
    // format that would be easier to be deserialized into a tree map.
    private void initDictionary()
    {
        new AsyncTask<Void, Integer, Boolean>()
        {
            @Override
            protected Boolean doInBackground(final Void... voids) {
                InputStream inputStream = getResources().openRawResource(R.raw.dictionary);
                InputStreamReader streamReader = new InputStreamReader(inputStream);
                BufferedReader bufferReader = new BufferedReader(streamReader);

                int progressInLines = 0;
                int progressInPercents = 0;
                String line;
                try
                {
                    while (( line = bufferReader.readLine()) != null)
                    {
                        mDictionary.add(line);
                        progressInLines++;

                        if (Math.floor(100*progressInLines/WORDS_IN_LANGUAGE) > progressInPercents)
                        {
                            progressInPercents++;
                            publishProgress(progressInPercents);
                        }
                    }
                }
                catch (final IOException e)
                {
                    return false;
                }

                return true;
            }

            @Override
            protected void onProgressUpdate(final Integer... progress)
            {
                display(getString(R.string.loading_progress, progress));
                return;
            }

            @Override
            protected void onPostExecute(final Boolean result)
            {
                if (result)
                {
                    mDictionaryReady = true;
                    display(getString(R.string.ready));
                }
                else
                {
                    display(getString(R.string.dictionary_error));
                }
            }
        }.execute();
    }

    /**
     * Taking the input string and computes the words.
     *
     * @param view Button which onclick triggered a call to this function.
     */
    public void findWords(final View view)
    {
        final long startTime = System.currentTimeMillis();
        if (mDictionaryReady)
        {
            final String inputString = mUserInput.getText().toString();

            mSolutions.clear();

            computePermutations(inputString);
            displayWords();

            final long totalTime = (System.currentTimeMillis() - startTime) / 1000;
            mResults.setText(getString(R.string.result_count, mSolutions.size(), totalTime));
        }
        else
        {
            Toast.makeText(this, getString(R.string.please_wait), Toast.LENGTH_LONG).show();
        }
    }

    // Finds all permutations of letters, which are potential words
    public static void computePermutations(String str)
    {
        testPermutation("", str);
    }

    /**
     * Pretty good snipet from
     * http://stackoverflow.com/questions/4240080/generating-all-permutations-of-a-given-string
     * My own solution would probably have been slightly inferior.
     *
     * @param prefix Part of the word that won't change in this tree of the recursion
     * @param str End of the string that still has to be "permuted"
     **/
    private static void testPermutation(String prefix, String str)
    {
        int n = str.length();
        if (n == 0)
        {
            // This is a permutation, see if it's a word
            checkIfWord(prefix);
        }
        else
        {
            for (int i = 0; i < n; i++)
            {
                // While going through the characters in str, take one at random and place it first
                // right behind the prefix, then iterate on that new string, i.e "permute" it.
                testPermutation(prefix + str.charAt(i), str.substring(0, i) + str.substring(i + 1, n));
                testPermutation("" + str.charAt(i), str.substring(0, i) + str.substring(i + 1, n));
            }
        }
    }

    /**
     * Test a potential word
     * @param potentialWord String of letters
     */
    private static void checkIfWord(final String potentialWord)
    {
        // Pretty self-explanatory, if the string is a word, add it to the solutions!
        if (mDictionary.contains(potentialWord))
        {
            mSolutions.add(potentialWord);
        }
    }

    /**
     * Not the most elegant way of displaying the results, but I don't think that was the point.
     */
    private void displayWords()
    {
        String resultDisplay = "";

        for(String word: mSolutions)
        {
            resultDisplay += (resultDisplay.isEmpty()) ? word : "\n" + word;
        }

        display(resultDisplay);
    }
}
