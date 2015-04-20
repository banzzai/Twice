package interview.twice.com.twice;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends Activity {
    private static final int SEARCH_ACTION = 1;
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

    // Comparator that sorts String by size, then alphabetical order
    private static Comparator resultComparator = new Comparator<String>()
    {
        @Override
        public int compare(final String leftString, final String rightString)
        {
            final int lengthDifference = rightString.length() - leftString.length();
            return lengthDifference == 0 ? leftString.compareTo(rightString) : lengthDifference;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);

        mUserInput = (EditText) findViewById(R.id.textInput);
        mDisplay = (TextView) findViewById(R.id.display);
        mResults = (TextView) findViewById(R.id.results);

        mDisplay.setMovementMethod(new ScrollingMovementMethod());

        mUserInput.setImeActionLabel(getString(android.R.string.search_go), EditorInfo.IME_ACTION_GO);
        mUserInput.setOnEditorActionListener(
                new EditText.OnEditorActionListener()
                {
                    @Override
                    public boolean onEditorAction(final TextView editText, final int actionId,
                                                  final KeyEvent event)
                    {
                        if (actionId == EditorInfo.IME_ACTION_GO)
                        {
                            findWords(editText);
                            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                            return true;
                        }
                        return false;
                    }
                });

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

                // Original code was only creating permutations of the entire set, but we want to
                // consider permutations including any number of characters, which this
                // additional line will help with.
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

        // Using a List to sort results alphabetically.
        List<String> sortedList = new ArrayList(mSolutions);
        // Comparator that sorts String by size, then alphabetical order
        Collections.sort(sortedList, resultComparator);

        int modulo = 0;
        for(String word: sortedList)
        {
            resultDisplay += (resultDisplay.isEmpty()) ? word : (modulo%5==0?"\n":"       ") + word;
            modulo++;
        }

        display(resultDisplay);
    }
}
