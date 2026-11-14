package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String GLOBAL_API_URL = "https://api.exchangerate-api.com/v4/latest/USD";
    private static final String NOBITEX_API_URL = "https://api.nobitex.ir/market/stats?srcCurrency=usdt&dstCurrency=irt";

    private TextView tvExchangeRate;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvExchangeRate = findViewById(R.id.tvExchangeRate);
        Button btnRefresh = findViewById(R.id.btnRefresh);

        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(this);

        btnRefresh.setOnClickListener(v -> fetchExchangeRates());

        // Fetch rates on initial load
        fetchExchangeRates();
    }

    private void fetchExchangeRates() {
        tvExchangeRate.setText(R.string.initial_rate_text);

        // 1. Fetch Global Rates
        JsonObjectRequest globalRequest = new JsonObjectRequest(
                Request.Method.GET,
                GLOBAL_API_URL,
                null,
                globalResponse -> {
                    try {
                        JSONObject rates = globalResponse.getJSONObject("rates");
                        double eur = rates.optDouble("EUR", 0);
                        double gbp = rates.optDouble("GBP", 0);
                        double cad = rates.optDouble("CAD", 0);
                        double jpy = rates.optDouble("JPY", 0);
                        String date = globalResponse.optString("date", "Today");

                        // 2. Fetch Live Market Toman/Rial Rate
                        fetchNobitexMarketRate(eur, gbp, cad, jpy, date);

                    } catch (JSONException e) {
                        tvExchangeRate.setText("Error parsing global exchange rates.");
                        Toast.makeText(MainActivity.this, "Parsing Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    tvExchangeRate.setText("Failed to load exchange rates.");
                    Toast.makeText(MainActivity.this, "Network Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );

        requestQueue.add(globalRequest);
    }

    private void fetchNobitexMarketRate(double eur, double gbp, double cad, double jpy, String date) {
        JsonObjectRequest nobitexRequest = new JsonObjectRequest(
                Request.Method.GET,
                NOBITEX_API_URL,
                null,
                nobitexResponse -> {
                    long rialVal = -1;
                    try {
                        JSONObject stats = nobitexResponse.getJSONObject("stats");
                        JSONObject usdtIrt = stats.getJSONObject("usdt-irt");
                        String latestRialStr = usdtIrt.getString("latest");
                        rialVal = Long.parseLong(latestRialStr);
                    } catch (Exception ignored) {
                    }

                    displayRates(eur, gbp, cad, jpy, rialVal, date);
                },
                error -> {
                    // Fallback if market rate fetch fails
                    displayRates(eur, gbp, cad, jpy, -1, date);
                }
        );

        requestQueue.add(nobitexRequest);
    }

    private void displayRates(double eur, double gbp, double cad, double jpy, long rialVal, String date) {
        NumberFormat formatter = NumberFormat.getInstance(Locale.US);
        String irrStr = rialVal > 0 ? formatter.format(rialVal) + " IRR" : "Unavailable";
        String tomanStr = rialVal > 0 ? formatter.format(rialVal / 10) + " Toman" : "Unavailable";

        String resultText = String.format(Locale.US,
                "Base Currency: USD\nDate: %s\n\n" +
                "Live Market Rates:\n" +
                "1 USD = %s\n" +
                "1 USD = %s\n\n" +
                "Global Rates:\n" +
                "1 USD = %.2f EUR\n" +
                "1 USD = %.2f GBP\n" +
                "1 USD = %.2f CAD\n" +
                "1 USD = %.2f JPY",
                date, irrStr, tomanStr, eur, gbp, cad, jpy);

        tvExchangeRate.setText(resultText);
    }
}