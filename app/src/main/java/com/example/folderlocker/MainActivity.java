package com.example.folderlocker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_OPEN_DIRECTORY = 1;
    private static final int REQUEST_CODE_CREATE_FILE = 2;

    private Button lockButton;
    private Button unlockButton;
    private Button allowButton;

    private String lockedFolderPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lockButton = findViewById(R.id.lock_button);
        unlockButton = findViewById(R.id.unlock_button);
        allowButton = findViewById(R.id.allow_button);

        lockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDirectory();
            }
        });

        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unlockFolder();
            }
        });

        allowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                allowFolder();
            }
        });
    }

    private void openDirectory() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
    }

    private void unlockFolder() {
        if (lockedFolderPath == null) {
            Toast.makeText(this, "No folder is currently locked", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Implement password input and validation logic here

        if (isFolderLocked(lockedFolderPath)) {
            if (unlockDirectory(lockedFolderPath)) {
                Toast.makeText(this, "Folder unlocked successfully", Toast.LENGTH_SHORT).show();
                lockedFolderPath = null;
            } else {
                Toast.makeText(this, "Failed to unlock the folder", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Folder is already unlocked", Toast.LENGTH_SHORT).show();
        }
    }

    private void allowFolder() {
        if (lockedFolderPath == null) {
            Toast.makeText(this, "No folder is currently locked", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Implement password input and validation logic here

        if (isFolderLocked(lockedFolderPath)) {
            Toast.makeText(this, "Please unlock the folder before allowing access", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Access granted to the folder", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isFolderLocked(String folderPath) {
        File folder = new File(folderPath);
        return folder.exists();
    }

    private boolean unlockDirectory(String lockedFolderPath) {
        File lockedFolder = new File(lockedFolderPath);
        if (lockedFolder.exists()) {
            String unlockedFolderPath = lockedFolder.getParent() + "/unlocked_" + lockedFolder.getName();
            File unlockedFolder = new File(unlockedFolderPath);

            if (unlockedFolder.exists()) {
                Toast.makeText(this, "Folder is already unlocked", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (unlockedFolder.mkdir()) {
                File[] files = lockedFolder.listFiles();

                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            String subFolderPath = unlockedFolderPath + "/" + file.getName();
                            File subFolder = new File(subFolderPath);

                            if (subFolder.mkdir()) {
                                if (!copyFiles(file.listFiles(), subFolderPath)) {
                                    Toast.makeText(this, "Failed to unlock the folder", Toast.LENGTH_SHORT).show();
                                    return false;
                                }
                            } else {
                                Toast.makeText(this, "Failed to unlock the folder", Toast.LENGTH_SHORT).show();
                                return false;
                            }
                        } else {
                            String filePath = unlockedFolderPath + "/" + file.getName();

                            if (!copyFile(file, filePath)) {
                                Toast.makeText(this, "Failed to unlock the folder", Toast.LENGTH_SHORT).show();
                                return false;
                            }
                        }
                    }
                }

                return true;
            } else {
                Toast.makeText(this, "Failed to unlock the folder", Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            Toast.makeText(this, "Folder does not exist", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean copyFiles(File[] files, String destinationPath) {
        boolean success = true;

        for (File file : files) {
            String filePath = destinationPath + "/" + file.getName();
            if (!copyFile(file, filePath)) {
                success = false;
            }
        }

        return success;
    }

    private boolean copyFile(File sourceFile, String destinationPath) {
        File destinationFile = new File(destinationPath);

        try {
            FileUtil.copyFile(sourceFile, destinationFile);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_OPEN_DIRECTORY && data != null) {
                Uri treeUri = data.getData();
                String folderPath = getFullPathFromTreeUri(this, treeUri);

                if (folderPath != null) {
                    if (!isFolderLocked(folderPath)) {
                        lockedFolderPath = folderPath;
                        Toast.makeText(this, "Folder locked successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Folder is already locked", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Failed to lock the folder", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Get the full path of a file from the given tree URI.
     *
     * @param context The context.
     * @param treeUri The tree URI of the file.
     * @return The full path of the file.
     */
    public static String getFullPathFromTreeUri(@NonNull Context context, @NonNull Uri treeUri) {
        if (DocumentsContract.isTreeUri(treeUri)) {
            String volumePath = getVolumePath(context, treeUri);
            String documentPath = getDocumentPath(context, treeUri);
            if (volumePath != null && documentPath != null) {
                return volumePath + documentPath;
            }
        }
        return null;
    }

    /**
     * Get the volume path from the given tree URI.
     *
     * @param context The context.
     * @param treeUri The tree URI.
     * @return The volume path.
     */
    @Nullable
    private static String getVolumePath(@NonNull Context context, @NonNull Uri treeUri) {
        String volumePath = null;
        String volumeId = DocumentsContract.getTreeDocumentId(treeUri);
        if (volumeId != null) {
            String[] split = volumeId.split(":");
            if (split.length > 0) {
                String volume = split[0];
                if ("primary".equalsIgnoreCase(volume)) {
                    volumePath = Objects.requireNonNull(context.getExternalFilesDir(null)).getAbsolutePath();
                } else {
                    String[] projection = {MediaStore.MediaColumns.DATA};
                    String selection = MediaStore.MediaColumns.DISPLAY_NAME + " LIKE '%" + volume + "%'";
                    Uri externalUri = MediaStore.Files.getContentUri("external");
                    Cursor cursor = context.getContentResolver().query(externalUri, projection, selection, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                        volumePath = cursor.getString(columnIndex);
                        cursor.close();
                    }
                }
            }
        }
        return volumePath;
    }

    /**
     * Get the document path from the given tree URI.
     *
     * @param context The context.
     * @param treeUri The tree URI.
     * @return The document path.
     */
    @Nullable
    private static String getDocumentPath(@NonNull Context context, @NonNull Uri treeUri) {
        String documentPath = null;
        ContentResolver contentResolver = context.getContentResolver();
        String documentId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
        Cursor cursor = contentResolver.query(documentUri, new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID},
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
            String documentIdPath = cursor.getString(columnIndex);
            cursor.close();
            String[] split = documentIdPath.split(":");
            if (split.length > 1) {
                documentPath = "/" + split[1];
            }
        }
        return documentPath;
    }
}
