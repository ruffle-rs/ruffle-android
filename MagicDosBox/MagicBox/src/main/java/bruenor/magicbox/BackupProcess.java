/*
 *  Copyright (C) 2013-2016 Antony Hornacek (magicbox@imejl.sk)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package bruenor.magicbox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import magiclib.CrossSettings;
import magiclib.Global;
import magiclib.IO.FileBrowser;
import magiclib.IO.Storages;
import magiclib.controls.Dialog;
import magiclib.controls.HelpViewer;
import magiclib.core.Backup;
import magiclib.locales.Localization;
import magiclib.logging.MessageInfo;

class BackupProcess extends PopupWindow {
    private ProgressBar progressBar;

    public Backup backupConfig;

    class BackupProcessAsync extends AsyncTask<String, Integer, String>
    {
        public boolean excludeScreenshots = false;
        private String zippedFolder;
        private String backupFile;
        private String baseFolder;
        private int maxSteps = 0;
        private int currentStep = 0;
        private boolean progressStarted = false;

        void countFiles(File src)
        {
            if(src.isDirectory())
            {
                File [] files = src.listFiles();

                if (files == null)
                    return;

                for (File file : files)
                {
                    if (file.isDirectory() && ((excludeScreenshots && file.getName().equals("Screenshots")) || (file.getName().equals("Temp"))))
                    {
                        continue;
                    }

                    countFiles(file);
                }
            }
            else
            {
                maxSteps++;
            }
        }

        private void addFileToZip(boolean useFilter, String path, String srcFile, ZipOutputStream zip) throws Exception
        {
            File folder = new File(srcFile);

            if (folder.isDirectory())
            {
                if ((excludeScreenshots && folder.getName().equals("Screenshots")) || (folder.getName().equals("Temp")))
                    return;

                addFolderToZip(useFilter, path, srcFile, zip);
                //addFolderToZip(useFilter, folder.getName(), srcFile, zip);
            }
            else
            {
                if (useFilter && folder.getName().equals("exportinfo.xml"))
                    return;

                byte[] buf = new byte[1024];
                int len;

                FileInputStream in = new FileInputStream(srcFile);

                /*if (path.startsWith(zippedFolder))
                {
                    String p = path.replaceFirst(zippedFolder, "") + "/" + folder.getName();
                    if (p.startsWith("/")) {
                        p = p.substring(1,p.length());
                    }

                    zip.putNextEntry(new ZipEntry(p));
                }
                else
                {*/
                    zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
                //}

                while ((len = in.read(buf)) > 0)
                {
                    zip.write(buf, 0, len);
                }

                in.close();
                zip.closeEntry();

                publishProgress(currentStep++, maxSteps);
            }
        }

        private void addFolderToZip(boolean useFilter, String path, String srcFolder, ZipOutputStream zip) throws Exception
        {
            File folder = new File(srcFolder);
            for (String fileName : folder.list())
            {
                if (path.equals("")) {
                    if (fileName.equals(zippedFolder)) {
                        addFileToZip(useFilter, folder.getName(), srcFolder + "/" + fileName, zip);
                    }
                } else if (path.equals("MagicBox" + zippedFolder)) {
                    if (fileName.equals("Data") ||
                            fileName.equals("collection.xml") ||
                            fileName.equals("cross_settings.xml") ||
                            fileName.equals("global.xml") ||
                            fileName.equals("mapper.xml") ||
                            fileName.startsWith("games.xml.bcp")) {
                        addFileToZip(useFilter, folder.getName(), srcFolder + "/" + fileName, zip);
                    }
                }
                else
                {
                    addFileToZip(useFilter, path + "/" + folder.getName(), srcFolder + "/" + fileName, zip);
                }
            }
        }
        private void zipFolder(String srcFolder, String destZipFile) throws Exception
        {
            baseFolder = srcFolder;

            FileOutputStream fileWriter = new FileOutputStream(destZipFile);
            ZipOutputStream zip = new ZipOutputStream(fileWriter);
            //zip.setLevel(Deflater.BEST_COMPRESSION);

            addFolderToZip(true, "", srcFolder, zip);

            zip.flush();
            zip.close();
        }

        @Override
        protected String doInBackground(String... params) {
            try
            {
                countFiles(new File(params[0]));

                zippedFolder = (AppGlobal.isDonated)? "Games":"GamesLite";

                zipFolder(params[0], params[1]);
                backupFile = params[1];

                return params[1];
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return "ERROR";
        }

        protected void onProgressUpdate(Integer... progress)
        {
            if (!progressStarted)
            {
                progressStarted = true;
                progressBar.setMax(progress[1]);
            }

            progressBar.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result)
        {
            if (!result.equals("ERROR")) {
                backupConfig.save();

                if (backupConfig.createLocalBackupEnabled()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(AppGlobal.context);
                    builder.setTitle(magiclib.R.string.app_name);
                    builder.setMessage(Localization.getString("msg_backup_success") +  " " + Localization.getString("msg_share_file"));
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AppGlobal.shareFile(new File(backupFile));
                        }
                    });

                    builder.setNegativeButton("Cancel", null);
                    builder.create().show();
                } else {
                    AppGlobal.shareFile(new File(backupFile));
                }
            } else {
                MessageInfo.info("msg_backup_failed");
            }

            dismiss();
        }
    }

    public BackupProcess(View popupView) {
        super(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
    }

    public void backup() {
        AppGlobal.clearTempFolder();

        progressBar = (ProgressBar)getContentView().findViewById(R.id.backup_process_progressbar);

        String extension = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        BackupProcessAsync backuper = new BackupProcessAsync();

        String backupDir;

        if (backupConfig.createLocalBackupEnabled()) {
            backupDir = backupConfig.getBackupDir();
        } else {
            backupDir = AppGlobal.appTempPath;
        }

        if (!backupDir.endsWith("/")) {
            backupDir+="/";
        }

        //backuper.execute(AppGlobal.gamesRootPath.substring(0,AppGlobal.gamesRootPath.length() - 1), backupDir + "MagicDosboxBkp_" + extension + ".zip");
        backuper.execute(AppGlobal.appPath.substring(0, AppGlobal.appPath.length() - 1), backupDir + "MagicDosboxBkp_" + extension + ".zip");
    }
}

class BackupDialog extends Dialog {
    private Backup bcp;
    private EditText localDir;
    private CheckBox createLocalBackup;

    private boolean reminderEnabled;
    private int nextRemindInDays;
    private Date now;
    private boolean backupExists;
    private boolean upToDate;

    @Override
    public void onSetLocalizedLayout()
    {
        localize(R.id.backup_title, "backup_maininfo");
        localize(R.id.backup_localbackup_title, "backup_localbackup_title");
        localize(R.id.backup_localbackup_enabled, "common_enabled");
        localize(R.id.backup_localbackup_choosedir, "common_choose");
        localize(R.id.backup_backupremind_title, "backup_backupremind_title");
        localize(R.id.backup_createbackup, "backup_createbackup_title");
        localize(R.id.backup_reminder_text, "common_reminder");
    }

    public BackupDialog() {
        super(AppGlobal.context);

        setContentView(R.layout.backup);
        setCaption("common_backup");

        View.OnClickListener onClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.backup_createbackup: {
                        String d = localDir.getText().toString().trim();

                        if (createLocalBackup.isChecked() && (d.equals("") || !(new File(d).exists()))) {
                            MessageInfo.info("msg_backupdir_notvalid");
                            return;
                        }

                        bcp.clearDateChange();// .setDateChange(null);
                        bcp.setBackupDir(localDir.getText().toString());
                        bcp.setCreateLocalBackup(createLocalBackup.isChecked());
                        bcp.setRemind(reminderEnabled);
                        bcp.setDaysRemind(7);

                        dismiss();

                        LayoutInflater inflater = (LayoutInflater) Global.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        View view = inflater.inflate(R.layout.backup_process, null);
                        BackupProcess process = new BackupProcess(view);
                        process.backupConfig = bcp;
                        bcp = null;
                        process.showAtLocation(((Activity) AppGlobal.context).getWindow().getDecorView().getRootView(), Gravity.CENTER, 0, 0);
                        process.backup();
                        break;
                    }
                    case R.id.backup_localbackup_choosedir: {
                        Storages.onDrivePick(getContext(), true, new Storages.onDrivePickListener() {
                            @Override
                            public void onPick(String drive) {
                                FileBrowser fb;

                                fb = new FileBrowser(getContext(), drive, null, true);

                                fb.setCaption("fb_caption_choose_folder");
                                fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener() {
                                    @Override
                                    public void onPick(String selected) {
                                        localDir.setText(selected);
                                        bcp.setBackupDir(selected);
                                    }
                                });

                                fb.show();
                            }
                        });
                        break;
                    }
                    case R.id.backup_help:{
                        HelpViewer hlp = new HelpViewer("common_help", null, "help/tips/collection/backup/backup.html", CrossSettings.showCollectionToolTip, true, false);
                        hlp.hideNavigationPanel();
                        hlp.show();
                        break;
                    }
                    case R.id.backup_reminder:{
                        RemindDialog rd = new RemindDialog(getContext(), upToDate, reminderEnabled, nextRemindInDays);
                        rd.setOnRemindEventListener(new RemindDialog.OnRemindEventListener() {
                            @Override
                            public void onPick(boolean enabled, int daysLeft) {
                                reminderEnabled = enabled;
                                nextRemindInDays = daysLeft;
                            }
                        });
                        rd.show();
                        break;
                    }
                    case R.id.backup_confirm: {
                        bcp.setBackupDir(localDir.getText().toString());
                        bcp.setCreateLocalBackup(createLocalBackup.isChecked());
                        bcp.setRemind(reminderEnabled);
                        
                        if (bcp.getChangeDate() != null) {
                            int d = (int) ((now.getTime() - bcp.getChangeDate().getTime()) / (24 * 60 * 60 * 1000));
                            d += nextRemindInDays;

                            bcp.setDaysRemind(d);
                        }

                        bcp.save();
                        dismiss();

                        break;
                    }
                }
            }
        };

        findViewById(R.id.backup_confirm).setOnClickListener(onClick);
        findViewById(R.id.backup_localbackup_choosedir).setOnClickListener(onClick);
        findViewById(R.id.backup_help).setOnClickListener(onClick);
        findViewById(R.id.backup_createbackup).setOnClickListener(onClick);
        findViewById(R.id.backup_reminder).setOnClickListener(onClick);

        localDir = (EditText)findViewById(R.id.backup_localbackup_dir);
        createLocalBackup = (CheckBox)findViewById(R.id.backup_localbackup_enabled);

        bcp = new Backup();
        backupExists = bcp.existBackupConfig();

        if (backupExists) {
            bcp.loadConfigFile();
        }

        upToDate = backupExists && bcp.getChangeDate()==null;

        localDir.setText(bcp.getBackupDir());
        createLocalBackup.setChecked(bcp.createLocalBackupEnabled());

        reminderEnabled = bcp.isRemindOn();

        now = new Date();

        if (!upToDate) {
            nextRemindInDays = bcp.getDaysRemind() - (int) ((now.getTime() - bcp.getChangeDate().getTime()) / (24 * 60 * 60 * 1000));
            if (nextRemindInDays < 0) {
                nextRemindInDays = 0;
            }
        }
    }
}

class RemindDialog extends Dialog {

    abstract interface OnRemindEventListener
    {
        public abstract void onPick(boolean enabled, int daysLeft);
    }

    private CheckBox reminderEnabled;
    private TextView reminderDaysTitle;
    private boolean upToDate;
    private int nextRemindInDays;
    private OnRemindEventListener event;

    @Override
    public void onSetLocalizedLayout()
    {
        localize(R.id.reminder_uptodate, "backup_uptodate");
        localize(R.id.reminder_enabled, "common_enabled");
        localize(R.id.reminder_next, "reminder_daystoremind");
    }

    public void setOnRemindEventListener(OnRemindEventListener event) {
        this.event = event;
    }

    public RemindDialog(Context context, boolean upToDate, boolean enabled, int daysLeft) {
        super(context);

        setContentView(R.layout.remind);
        setCaption("common_reminder");

        this.upToDate = upToDate;
        this.nextRemindInDays = daysLeft;

        View.OnClickListener onClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.reminder_days_minus: {
                        if (nextRemindInDays == 0) {
                            break;
                        }

                        nextRemindInDays--;
                        updateRemingDaysTitle();
                        break;
                    }
                    case R.id.reminder_days_plus: {
                        if (nextRemindInDays >= 30) {
                            break;
                        }

                        nextRemindInDays++;
                        updateRemingDaysTitle();
                        break;
                    }
                    case R.id.reminder_confirm:{
                        if (event!=null) {
                            event.onPick(reminderEnabled.isChecked(), nextRemindInDays);
                        }
                        dismiss();
                        break;
                    }
                }
            }
        };

        reminderEnabled = (CheckBox)findViewById(R.id.reminder_enabled);
        reminderEnabled.setChecked(enabled);

        if (upToDate) {
            findViewById(R.id.reminder_days_panel).setVisibility(View.GONE);
            findViewById(R.id.reminder_uptodate).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.reminder_uptodate).setVisibility(View.GONE);

            findViewById(R.id.reminder_days_minus).setOnClickListener(onClick);
            findViewById(R.id.reminder_days_plus).setOnClickListener(onClick);

            reminderDaysTitle = (TextView)findViewById(R.id.reminder_days_value);
            updateRemingDaysTitle();
        }

        findViewById(R.id.reminder_confirm).setOnClickListener(onClick);
    }

    private void updateRemingDaysTitle() {
        reminderDaysTitle.setText("" + nextRemindInDays);
    }
}