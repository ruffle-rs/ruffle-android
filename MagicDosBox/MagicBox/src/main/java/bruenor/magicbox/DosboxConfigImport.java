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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.LinkedList;

import magiclib.Global;
import magiclib.IO.FileBrowser;
import magiclib.IO.Storages;
import magiclib.controls.Dialog;
import magiclib.core.LnkReader;
import magiclib.dosbox.DosboxImport;

public class DosboxConfigImport extends Dialog {
    enum State {
        home,
        menu1,
        shortcut_pick,
        game_folder_pick
    }

    private State state;

    private ImageButton goBack;
    private ImageButton goForward;
    private ScrollView contentHolder;
    private Button pickShortcut;
    private Button pickGamefld;
    private TextView shortcutResult;
    private TextView gamefldInfo;

    private View homeView;
    private View menu1View;
    private View shortcutPickView;
    private View gameFolderPickView;
    private boolean selectedShortcut = true;

    private View.OnClickListener onClick;
    private LayoutInflater inflater;
    private LinkedList<String> lnkResults;
    private LinkedList<String> fileFilter;

    public DosboxConfigImport() {
        super(Global.context);

        setContentView(R.layout.dosbox_import_contentholder);

        goBack = (ImageButton)findViewById(R.id.go_back);
        goBack.setOnClickListener(getOnClick());

        goForward = (ImageButton)findViewById(R.id.go_forward);
        goForward.setOnClickListener(getOnClick());

        contentHolder = (ScrollView)findViewById(R.id.dbximp_content);

        inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        setState(State.home);
    }

    private void setState(State state) {
        this.state = state;

        switch (state) {
            case home: {
                if (homeView == null) {
                    homeView = inflater.inflate(R.layout.dosbox_import_home, null);
                }

                contentHolder.removeAllViews();
                contentHolder.addView(homeView);

                goBack.setVisibility(View.GONE);
                break;
            }
            case menu1: {
                if (menu1View == null) {
                    menu1View = inflater.inflate(R.layout.dosbox_import_menu1, null);

                    RadioGroup.OnCheckedChangeListener onRadioChange = new RadioGroup.OnCheckedChangeListener()
                    {
                        @Override
                        public void onCheckedChanged(RadioGroup radioGroup, int checkedId)
                        {
                            switch (checkedId) {
                                case R.id.dbximp_menu1_radio_shortcut: {
                                    selectedShortcut = true;
                                }
                                case R.id.dbximp_menu1_radio_config: {
                                    selectedShortcut = false;
                                }
                            }
                        }
                    };

                }

                contentHolder.removeAllViews();
                contentHolder.addView(menu1View);

                goBack.setVisibility(View.VISIBLE);
                break;
            }
            case shortcut_pick: {
                if (shortcutPickView == null) {
                    shortcutPickView = inflater.inflate(R.layout.dosbox_import_shortcutpick, null);

                    pickShortcut = (Button)shortcutPickView.findViewById(R.id.dbximp_pickshortcut);
                    pickShortcut.setOnClickListener(getOnClick());

                    shortcutResult = (TextView)shortcutPickView.findViewById(R.id.dbximp_shortcutpick_result);
                }

                contentHolder.removeAllViews();
                contentHolder.addView(shortcutPickView);

                shortcutResult.setText("");
                break;
            }
            case game_folder_pick: {
                if (gameFolderPickView == null) {
                    gameFolderPickView = inflater.inflate(R.layout.dosbox_import_gamefldpick, null);

                    pickGamefld = (Button)gameFolderPickView.findViewById(R.id.dbximp_gamefldpick);
                    pickGamefld.setOnClickListener(getOnClick());

                    gamefldInfo = (TextView)gameFolderPickView.findViewById(R.id.dbximp_gamefldpickinfo);
                }

                contentHolder.removeAllViews();
                contentHolder.addView(gameFolderPickView);
            }
        }
    }

    private View.OnClickListener getOnClick() {
        if (onClick == null) {
            onClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (v.getId()) {
                        case R.id.go_back: {
                            goBack();
                            break;
                        }
                        case R.id.go_forward: {
                            goForward();
                            break;
                        }
                        case R.id.dbximp_pickshortcut:{
                            pickShortcut();
                            break;
                        }
                        case R.id.dbximp_gamefldpick: {
                            pickGameFolder();
                        }
                    }
                }
            };
        }

        return  onClick;
    }

    private void goBack() {
        switch (state) {
            case menu1: {
                setState(State.home);
                break;
            }
            case shortcut_pick: {
                setState(State.menu1);
                break;
            }
        }
    }

    private void goForward() {
        switch (state) {
            case home: {
                setState(State.menu1);
                break;
            }
            case menu1:{
                if (selectedShortcut) {
                    setState(State.shortcut_pick);
                } else {

                }
                break;
            }
        }
    }

    private void pickShortcut() {
        Storages.onDrivePick(getContext(), new Storages.onDrivePickListener() {
            @Override
            public void onPick(String drive) {
                FileBrowser fb = new FileBrowser(getContext(), drive, new String[]{".lnk"});
                fb.setCaption("fb_caption_choose_html");
                fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener() {
                    @Override
                    public void onPick(String selected) {
                        parseShortcut(new File(selected));
                    }
                });
                fb.show();
            }
        });
    }

    private void pickGameFolder() {
        Storages.onDrivePick(getContext(), new Storages.onDrivePickListener() {
            @Override
            public void onPick(String drive) {
                String [] filter = fileFilter.toArray(new String[fileFilter.size()]);

                FileBrowser fb = new FileBrowser(getContext(), drive, filter);
                fb.setCaption("fb_caption_choose_html");
                fb.setOnPickFileEvent(new FileBrowser.OnPickFileClickListener() {
                    @Override
                    public void onPick(String selected) {
                        parseShortcut(new File(selected));
                    }
                });
                fb.show();
            }
        });
    }

    private void parseShortcut(File shortcut) {
        boolean error = true;
        LnkReader r = null;
        try {
            r = new LnkReader(shortcut);
            error = r.getCommandLine() == null;
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        /*catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }*/

        if (error) {
            shortcutResult.setText("Error!");
        } else {
            String [] arguments = r.getCommandLine().split(" ");
            boolean grabNextValue = false;

            if (arguments.length == 0) {
                shortcutResult.setText("Error!");
                return;
            }

            for (String arg : arguments) {
                if (arg == null || arg.equals(""))
                    continue;

                if (arg.equals("-conf")) {
                    grabNextValue = true;
                    continue;
                }

                if (grabNextValue) {
                    if (lnkResults == null) {
                        lnkResults = new LinkedList<>();

                        fileFilter = new LinkedList<>();
                    }

                    lnkResults.add(arg);

                    arg = arg.replace("\\", "");
                    arg = arg.replace("..", "");
                    arg = arg.replace("\"", "");

                    fileFilter.add(arg);
                    grabNextValue =false;
                }
            }

            if (lnkResults == null || lnkResults.size() == 0) {
                shortcutResult.setText("Error!");
                return;
            }

            File parentFile = shortcut.getParentFile();

            if (simpleConfigLocate(parentFile)) {
                parseConfigFiles(parentFile);
            } else {
                setState(State.game_folder_pick);

                String msg;

                if (fileFilter.size() == 1) {
                    msg = "Please pick file listed below. Must be located in game folder : \n\n";
                } else {
                    msg = "Please pick one of files listed below. Files must be located in game folder : \n\n";
                }

                for (String f : fileFilter) {
                    msg += f + "\n";
                }

                gamefldInfo.setText(msg);
            }
        }
    }

    private boolean simpleConfigLocate(File dir) {
        for (String config : fileFilter) {
            File configFile = new File(dir, config);
            if (!configFile.exists()) {
                return false;
            }
        }
        return true;
    }

    private void parseConfigFiles(File dir) {
        int size = fileFilter.size();

        for (int i = 0; i < size; i++) {
            String config = fileFilter.get(i);

            DosboxImport imp = new DosboxImport();
            imp.parse(new File(dir, config), lnkResults.get(i));
        }
    }
}
