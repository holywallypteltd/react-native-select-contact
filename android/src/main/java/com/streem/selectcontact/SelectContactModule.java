package com.streem.selectcontact;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Contacts.Entity;
import android.util.Log;
import android.content.Context;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.ArrayList;

public class SelectContactModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    private static final String TAG = "SelectContactModule";
    private static final int CONTACT_REQUEST = 11112;
    private static final int REQUEST_CODE = 1;
    public static final String E_CONTACT_CANCELLED = "E_CONTACT_CANCELLED";
    public static final String E_CONTACT_NO_DATA = "E_CONTACT_NO_DATA";
    public static final String E_CONTACT_EXCEPTION = "E_CONTACT_EXCEPTION";
    public static final String E_CONTACT_PERMISSION = "E_CONTACT_PERMISSION";
    private Promise mContactsPromise;
    private final ContentResolver contentResolver;

    public SelectContactModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.contentResolver = getReactApplicationContext().getContentResolver();
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "SelectContact";
    }


    @ReactMethod
    public void openContactSelection(Promise contactsPromise) {
        mContactsPromise = contactsPromise;


        Uri uri = Uri.parse("content://contacts");
        Intent intent = new Intent(Intent.ACTION_PICK, uri);
        intent.setType(Phone.CONTENT_TYPE);
        Activity activity = getCurrentActivity();

        activity.startActivityForResult(intent, REQUEST_CODE);


    }

    /**
     * Lanch the contact picker, with the specified requestCode for returned data.
     *
     * @param contactsPromise - promise passed in from React Native.
     * @param requestCode     - request code to specify what contact data to return
     */
    private void launchPicker(Promise contactsPromise, int requestCode) {
        mContactsPromise = contactsPromise;
        Cursor cursor = this.contentResolver.query(Contacts.CONTENT_URI, null, null, null, null);

        if (cursor != null) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(Contacts.CONTENT_TYPE);
            Activity activity = getCurrentActivity();
            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivityForResult(intent, requestCode);
            }
            cursor.close();
        } else {
            mContactsPromise.reject(E_CONTACT_PERMISSION, "no permission");
        }
    }

//     @Override
//     protected void onActivityResult(int requestCode, int resultCode,
//             Intent intent) {
//         if (requestCode == REQUEST_CODE) {
//             if (resultCode == Activity.RESULT_OK) {
//                 Uri uri = intent.getData();
//                 String[] projection = { Phone.NUMBER, Phone.DISPLAY_NAME };
//
//                 Cursor cursor = getReactApplicationContext().getContentResolver().query(uri, projection,
//                         null, null, null);
//                 cursor.moveToFirst();
//
//                 int numberColumnIndex = cursor.getColumnIndex(Phone.NUMBER);
//                 String number = cursor.getString(numberColumnIndex);
//
//                 int nameColumnIndex = cursor.getColumnIndex(Phone.DISPLAY_NAME);
//                 String name = cursor.getString(nameColumnIndex);
//
//                 Log.d(TAG, "ZZZ number : " + number +" , name : "+name);
//
//             }
//         }
//     };

    private String getContactId(Uri contactUri) throws SelectContactException {
        Cursor cursor = this.contentResolver.query(contactUri, null, null, null, null);
        if (cursor == null || !cursor.moveToFirst()) {
            throw new SelectContactException(E_CONTACT_NO_DATA, "Contact Data Not Found");
        }

        return cursor.getString(cursor.getColumnIndex(Contacts._ID));
    }

    private Uri buildContactUri(String id) {
        return Uri
                .withAppendedPath(Contacts.CONTENT_URI, id)
                .buildUpon()
                .appendPath(Entity.CONTENT_DIRECTORY)
                .build();
    }

    private Cursor openContactQuery(Uri contactUri) throws SelectContactException {
        String[] projection = {
                Entity.MIMETYPE,
                Entity.DATA1,
                Entity.DATA2,
                Entity.DATA3
        };
        String sortOrder = Entity.RAW_CONTACT_ID + " ASC";
        Cursor cursor = this.contentResolver.query(contactUri, projection, null, null, sortOrder);
        if (cursor == null) {
            throw new SelectContactException(E_CONTACT_EXCEPTION, "Could not query contacts data. Unable to create cursor.");
        }

        return cursor;
    }

    private void addNameData(WritableMap contactData, Cursor cursor) {
        int displayNameIndex = cursor.getColumnIndex(StructuredName.DISPLAY_NAME);
        contactData.putString("name", cursor.getString(displayNameIndex));

        int givenNameColumn = cursor.getColumnIndex(StructuredName.GIVEN_NAME);
        if (givenNameColumn != -1) {
            String givenName = cursor.getString(givenNameColumn);
            contactData.putString("givenName", givenName);
        }

        int familyNameColumn = cursor.getColumnIndex(StructuredName.FAMILY_NAME);
        if (familyNameColumn != -1) {
            String familyName = cursor.getString(cursor.getColumnIndex(StructuredName.FAMILY_NAME));
            contactData.putString("familyName", familyName);
        }

        int middleNameColumn = cursor.getColumnIndex(StructuredName.MIDDLE_NAME);
        if (middleNameColumn != -1) {
            String middleName = cursor.getString(middleNameColumn);
            contactData.putString("middleName", middleName);
        }
    }

    private void addPostalData(WritableArray postalAddresses, Cursor cursor, Activity activity) {
        // we need to see if the postal address columns exist, if so, add them
        int formattedAddressColumn = cursor.getColumnIndex(StructuredPostal.FORMATTED_ADDRESS);
        int streetColumn = cursor.getColumnIndex(StructuredPostal.STREET);
        int cityColumn = cursor.getColumnIndex(StructuredPostal.CITY);
        int stateColumn = cursor.getColumnIndex(StructuredPostal.REGION);
        int postalCodeColumn = cursor.getColumnIndex(StructuredPostal.POSTCODE);
        int isoCountryCodeColumn = cursor.getColumnIndex(StructuredPostal.COUNTRY);

        WritableMap addressEntry = Arguments.createMap();
        if (formattedAddressColumn != -1) {
            addressEntry.putString("formattedAddress", cursor.getString(formattedAddressColumn));
        }
        if (streetColumn != -1) {
            addressEntry.putString("street", cursor.getString(streetColumn));
        }
        if (cityColumn != -1) {
            addressEntry.putString("city", cursor.getString(cityColumn));
        }
        if (stateColumn != -1) {
            addressEntry.putString("state", cursor.getString(stateColumn));
        }
        if (postalCodeColumn != -1) {
            addressEntry.putString("postalCode", cursor.getString(postalCodeColumn));
        }
        if (isoCountryCodeColumn != -1) {
            addressEntry.putString("isoCountryCode", cursor.getString(isoCountryCodeColumn));
        }

        // add the address type here
        int addressTypeColumn = cursor.getColumnIndex(StructuredPostal.TYPE);
        int addressLabelColumn = cursor.getColumnIndex(StructuredPostal.LABEL);
        if (addressTypeColumn != -1 && addressLabelColumn != -1) {
            String addressLabel = cursor.getString(addressLabelColumn);
            int addressType = cursor.getInt(addressTypeColumn);
            CharSequence typeLabel = StructuredPostal.getTypeLabel(activity.getResources(), addressType, addressLabel);
            addressEntry.putString("type", String.valueOf(typeLabel));
        }

        postalAddresses.pushMap(addressEntry);
    }

    private void addPhoneEntry(WritableArray phones, Cursor cursor, Activity activity) {
        String phoneNumber = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
        int phoneType = cursor.getInt(cursor.getColumnIndex(Phone.TYPE));
        String phoneLabel = cursor.getString(cursor.getColumnIndex(Phone.LABEL));
        CharSequence typeLabel = Phone.getTypeLabel(activity.getResources(), phoneType, phoneLabel);

        WritableMap phoneEntry = Arguments.createMap();
        phoneEntry.putString("number", phoneNumber);
        phoneEntry.putString("type", String.valueOf(typeLabel));

        phones.pushMap(phoneEntry);
    }

    private void addEmailEntry(WritableArray emails, Cursor cursor, Activity activity) {
        String emailAddress = cursor.getString(cursor.getColumnIndex(Email.ADDRESS));
        int emailType = cursor.getInt(cursor.getColumnIndex(Email.TYPE));
        String emailLabel = cursor.getString(cursor.getColumnIndex(Email.LABEL));
        CharSequence typeLabel = Email.getTypeLabel(activity.getResources(), emailType, emailLabel);

        WritableMap emailEntry = Arguments.createMap();
        emailEntry.putString("address", emailAddress);
        emailEntry.putString("type", String.valueOf(typeLabel));

        emails.pushMap(emailEntry);
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
        if (mContactsPromise == null || requestCode != 1) {
            return;
        }
        if (resultCode != Activity.RESULT_OK) {
            mContactsPromise.reject(E_CONTACT_CANCELLED, "Cancelled");
            return;
        }
        // Retrieve all possible data about contact and return as a JS object
        WritableMap contactData = Arguments.createMap();

        try {
            Uri uri = intent.getData();
            boolean foundData = false;


            Cursor cursor = getCurrentActivity().getContentResolver().query(uri, null, null, null, null);
            if (cursor == null) {
                mContactsPromise.reject(E_CONTACT_PERMISSION, "no permission");
                return;
            }
            ContentResolver cr = getCurrentActivity().getContentResolver();

            if (null != cursor && cursor.getCount() > 0) {
                cursor.moveToFirst();
                for (String column : cursor.getColumnNames()) {
                    Log.i(TAG, "contactPicked() uri column " + column + " : " + cursor.getString(cursor.getColumnIndex(column)));
                }
            }

            cursor.moveToFirst();
            String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

            Log.i(TAG, "contactPicked() uri id " + id);
            String contact_id = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
            Log.i(TAG, "contactPicked() uri contact id " + contact_id);
            // column index of the contact name
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            // column index of the phone number
            String phoneNo = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            //get Email id of selected contact....
            Log.e("ContactsFragment", "::>> " + id + name + phoneNo);

            Cursor cur1 = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", new String[]{contact_id}, null);
            String email = null;
            if (null != cur1 && cur1.getCount() > 0) {
                cur1.moveToFirst();
                for (String column : cur1.getColumnNames()) {
                    Log.i(TAG, "contactPicked() Email column " + column + " : " + cur1.getString(cur1.getColumnIndex(column)));
                    email = cur1.getString(cur1.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                }

                //HERE YOU GET name, phoneno & email of selected contact from contactlist.....
                Log.e("setcontactDetails", "::>>" + name + "\nPhoneno:" + phoneNo + "\nEmail: " + email);
            } else {
                Log.e("setcontactDetails", "::>>" + name + "\nPhoneno:" + phoneNo + "\nEmail: " + email);
            }
            WritableArray phones = Arguments.createArray();
            WritableArray emails = Arguments.createArray();
            WritableMap phoneEntry = Arguments.createMap();
            phoneEntry.putString("number", phoneNo);
            phones.pushMap(phoneEntry);

            WritableMap emailEntry = Arguments.createMap();
            emailEntry.putString("address", email);
            contactData.putString("recordId", id);

            contactData.putArray("phones", phones);
            mContactsPromise.resolve(contactData);


//
//            Cursor cursor = contentResolver.query(
//                    Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, ContactsContract.Contacts.Data.CONTENT_DIRECTORY),
//                    null,
//                    null,
//                    null,
//                    null
//            );
//
//            if (true) {
//                do {
//                    String mime = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID));
//                    switch (mime) {
//                        case StructuredName.CONTENT_ITEM_TYPE:
//                            addNameData(contactData, cursor);
//                            foundData = true;
//                            break;
//
//                        case StructuredPostal.CONTENT_ITEM_TYPE:
//                            addPostalData(postalAddresses, cursor, activity);
//                            foundData = true;
//                            break;
//
//                        case Phone.CONTENT_ITEM_TYPE:
//                            addPhoneEntry(phones, cursor, activity);
//                            foundData = true;
//                            break;
//
//                        case Email.CONTENT_ITEM_TYPE:
//                            addEmailEntry(emails, cursor, activity);
//                            foundData = true;
//                            break;
//                    }
//                } while (cursor.moveToNext());
//            }
//            cursor.close();
//
//            contactData.putArray("phones", phones);
//            contactData.putArray("emails", emails);
//            contactData.putArray("postalAddresses", postalAddresses);
//
//            if (foundData) {
//                mContactsPromise.resolve(contactData);
//            } else {
//                mContactsPromise.reject(E_CONTACT_NO_DATA, "No data found for contact");
//            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception reading from contacts", e);
            mContactsPromise.reject(E_CONTACT_EXCEPTION, e.getMessage());
        }
    }

    public ArrayList<String> getNameUsingContactId(String id) {

        ArrayList<String> phones = new ArrayList<String>();

        Cursor cursor = getReactApplicationContext().getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{id}, null);

        while (cursor.moveToNext()) {
            phones.add(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
        }

        cursor.close();
        return (phones);
    }

    public void onNewIntent(Intent intent) {

    }

    public static class SelectContactException extends Exception {
        private final String errorCode;

        public SelectContactException(String errorCode, String errorMessage) {
            super(errorMessage);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
