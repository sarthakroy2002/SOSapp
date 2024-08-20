package com.neolit.womensos;

import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class add extends AppCompatActivity {

    dbHelper databaseHelper;
    EditText mName, mNumber;
    Button btn;
    ListView mContacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add);

        databaseHelper = new dbHelper(this);

        mName = findViewById(R.id.mName);
        mNumber = findViewById(R.id.mNumber);
        btn = findViewById(R.id.btn);
        mContacts = findViewById(R.id.mContacts);

        loadContacts();

        btn.setOnClickListener(v -> {
            String name = mName.getText().toString();
            String number = mNumber.getText().toString();

            if (!name.isEmpty() && !number.isEmpty()) {
                boolean isInserted = databaseHelper.addContact(name, number);
                if (isInserted) {
                    Toast.makeText(add.this, "Contact Added", Toast.LENGTH_SHORT).show();
                    loadContacts();
                } else {
                    Toast.makeText(add.this, "Failed to Add Contact", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(add.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        registerForContextMenu(mContacts);
    }

    private void loadContacts() {
        Cursor cursor = databaseHelper.getAllContacts();
        ArrayList<String> contacts = new ArrayList<>();

        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex("name");
            int numberIndex = cursor.getColumnIndex("number");

            if (nameIndex >= 0 && numberIndex >= 0) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(nameIndex);
                    String number = cursor.getString(numberIndex);
                    contacts.add(name + " - " + number);
                }
            } else {
                Toast.makeText(this, "Error: Column not found", Toast.LENGTH_SHORT).show();
            }
            cursor.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, contacts);
        mContacts.setAdapter(adapter);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int id = item.getItemId();

        if (id == R.id.delete) {
            assert info != null;
            String contact = (String) mContacts.getItemAtPosition(info.position);
            String name = contact.split(" - ")[0];

            boolean isDeleted = databaseHelper.deleteContact(name);
            if (isDeleted) {
                loadContacts();
                Toast.makeText(this, "Contact Deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to Delete Contact", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onContextItemSelected(item);
    }

}
