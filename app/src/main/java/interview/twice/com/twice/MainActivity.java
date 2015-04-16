package interview.twice.com.twice;

import android.app.Activity;
import android.content.Context;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private EditText mUserInput;
    private TextView mDisplay;

    private Map<String, Boolean> mDictionary = new HashMap<String, Boolean>();
    private boolean mDictionaryReady = false;
    private static final int WORDS_IN_LANGUAGE = 172820;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUserInput = (EditText) findViewById(R.id.textInput);
        mDisplay = (TextView) findViewById(R.id.display);

        initDictionary();
        display(getString(R.string.loading));
    }

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
                        mDictionary.put(line, true);
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
        if (mDictionaryReady)
        {
            final String inputString = mUserInput.getText().toString();

            // Adding all letters to an array
            if (!inputString.isEmpty()) {
                ArrayList<Character> letters = new ArrayList<Character>();
                // We are going to ignore the non letter characters
                for (int i = 0; i < inputString.length(); i++) {
                    final char character = inputString.charAt(i);
                    if (Character.isLetter(character)) {
                        letters.add(character);
                    }
                }
            }
        }
        else
        {
            Toast.makeText(this, getString(R.string.please_wait), Toast.LENGTH_LONG).show();
        }
    }

    private void display(final String string)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDisplay.setText(string);
            }
        });
    }
}
