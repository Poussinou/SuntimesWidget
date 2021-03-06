/**
    Copyright (C) 2014 Forrest Guice
    This file is part of SuntimesWidget.

    SuntimesWidget is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SuntimesWidget is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SuntimesWidget.  If not, see <http://www.gnu.org/licenses/>.
*/ 

package com.forrestguice.suntimeswidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;

import com.forrestguice.suntimeswidget.calculator.SuntimesCalculatorDescriptor;
import com.forrestguice.suntimeswidget.getfix.GetFixUI;

import com.forrestguice.suntimeswidget.settings.AppSettings;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;
import com.forrestguice.suntimeswidget.settings.WidgetTimezones;

import com.forrestguice.suntimeswidget.settings.WidgetThemes;
import com.forrestguice.suntimeswidget.themes.SuntimesTheme;
import com.forrestguice.suntimeswidget.themes.SuntimesTheme.ThemeDescriptor;

import java.util.TimeZone;

/**
 * Main widget config activity.
 */
public class SuntimesConfigActivity extends AppCompatActivity
{
    protected static final String DIALOGTAG_ABOUT = "about";
    protected static final String DIALOGTAG_HELP = "help";

    protected int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    protected boolean reconfigure = false;

    protected Spinner spinner_calculatorMode;
    protected Spinner spinner_timeMode;
    protected ImageButton button_timeModeHelp;
    protected Spinner spinner_compareMode;

    protected Spinner spinner_onTap;
    protected EditText text_launchActivity;

    protected Spinner spinner_1x1mode;
    protected Spinner spinner_theme;
    protected CheckBox checkbox_allowResize;
    protected CheckBox checkbox_showTitle;

    protected TextView label_titleText;
    protected EditText text_titleText;

    protected LocationConfigView locationConfig;

    protected Spinner spinner_timezoneMode;

    protected LinearLayout layout_timezone;
    protected TextView label_timezone;
    protected Spinner spinner_timezone;

    protected LinearLayout layout_solartime;
    protected TextView label_solartime;
    protected Spinner spinner_solartime;

    protected String customTimezoneID;
    protected ActionMode.Callback spinner_timezone_actionMode;
    protected WidgetTimezones.TimeZoneItemAdapter spinner_timezone_adapter;

    protected ActionMode actionMode = null;

    public SuntimesConfigActivity()
    {
        super();
    }

    @Override
    public void onCreate(Bundle icicle)
    {
        setTheme(AppSettings.loadTheme(this));
        GetFixUI.themeIcons(this);

        super.onCreate(icicle);
        initLocale();
        setResult(RESULT_CANCELED);  // causes widget host to cancel if user presses back
        setContentView(R.layout.layout_settings);

        Context context = SuntimesConfigActivity.this;
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null)
        {
            appWidgetId = extras.getInt( AppWidgetManager.EXTRA_APPWIDGET_ID,
                                         AppWidgetManager.INVALID_APPWIDGET_ID );
            reconfigure = extras.getBoolean(WidgetSettings.ActionMode.ONTAP_LAUNCH_CONFIG.name(), false);
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
        {
            Log.w("CONFIG", "Invalid widget ID! returning early.");
            finish();
            return;
        }

        WidgetThemes.initThemes(context);

        initViews(context);
        loadSettings(context);
    }

    private void initLocale()
    {
        AppSettings.initLocale(this);
        WidgetSettings.initDefaults(this);
        WidgetSettings.initDisplayStrings(this);
        WidgetTimezones.TimeZoneSort.initDisplayStrings(this);
    }

    @Override
    public void onDestroy()
    {
        locationConfig.cancelGetFix();
        super.onDestroy();
    }

    /**
     * Save settings (as represented by the state of the config UI).
     * @param context the android application context
     */
    protected void saveSettings( Context context )
    {
        saveGeneralSettings(context);
        locationConfig.saveSettings(context);
        saveTimezoneSettings(context);
        saveAppearanceSettings(context);
        saveActionSettings(context);
    }

    /**
     * Load settings (update the state of the config UI).
     * @param context
     */
    protected void loadSettings( Context context )
    {
        loadGeneralSettings(context);
        loadAppearanceSettings(context);
        locationConfig.loadSettings(context);
        loadTimezoneSettings(context);
        loadActionSettings(context);
    }


    protected ArrayAdapter<WidgetSettings.ActionMode> createAdapter_actionMode()
    {
        ArrayAdapter<WidgetSettings.ActionMode> adapter = new ArrayAdapter<WidgetSettings.ActionMode>(this, R.layout.layout_listitem_oneline, supportedActionModes());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }
    protected WidgetSettings.ActionMode[] supportedActionModes()
    {
        WidgetSettings.ActionMode[] allModes = WidgetSettings.ActionMode.values();
        WidgetSettings.ActionMode[] supportedModes = new WidgetSettings.ActionMode[allModes.length - 1];
        System.arraycopy(allModes, 0, supportedModes, 0, supportedModes.length);
        return supportedModes;
    }


    protected void initViews( Context context )
    {
        //
        // widget: add button
        //
        button_addWidget = (Button)findViewById(R.id.add_button);
        button_addWidget.setEnabled(false);   // enabled later after timezones fully loaded
        if (button_addWidget != null)
        {
            button_addWidget.setOnClickListener(onAddButtonClickListener);
        }

        if (reconfigure)
        {
            setActionButtonText(getString(R.string.configAction_reconfigWidget_short));
            setConfigActivityTitle(getString(R.string.configAction_reconfigWidget));
        }

        //
        // widget: onTap
        //
        spinner_onTap = (Spinner)findViewById(R.id.appwidget_action_onTap);
        spinner_onTap.setAdapter(createAdapter_actionMode());
        spinner_onTap.setOnItemSelectedListener(onActionModeListener);

        //
        // widget: onTap launchActivity
        //
        text_launchActivity = (EditText)findViewById(R.id.appwidget_action_launch);

        ImageButton button_launchAppHelp = (ImageButton)findViewById(R.id.appwidget_action_launch_helpButton);
        if (button_launchAppHelp != null)
        {
            button_launchAppHelp.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    HelpDialog helpDialog = new HelpDialog();
                    helpDialog.setContent(getString(R.string.help_action_launch));
                    helpDialog.show(getSupportFragmentManager(), DIALOGTAG_HELP);
                }
            });
        }

        //
        // widget: theme
        //
        spinner_theme = (Spinner)findViewById(R.id.appwidget_appearance_theme);
        if (spinner_theme != null)
        {
            ArrayAdapter<ThemeDescriptor> spinner_themeAdapter;
            spinner_themeAdapter = new ArrayAdapter<ThemeDescriptor>(this, R.layout.layout_listitem_oneline, WidgetThemes.values());
            spinner_themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_theme.setAdapter(spinner_themeAdapter);
        }

        //
        // widget: source
        //
        spinner_calculatorMode = (Spinner)findViewById(R.id.appwidget_general_calculator);
        if (spinner_calculatorMode != null)
        {
            ArrayAdapter<SuntimesCalculatorDescriptor> spinner_calculatorModeAdapter;
            spinner_calculatorModeAdapter = new ArrayAdapter<SuntimesCalculatorDescriptor>(this, R.layout.layout_listitem_oneline, SuntimesCalculatorDescriptor.values());
            spinner_calculatorModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_calculatorMode.setAdapter(spinner_calculatorModeAdapter);
        }

        //
        // widget: time mode
        //
        spinner_timeMode = (Spinner)findViewById(R.id.appwidget_general_timeMode);
        button_timeModeHelp = (ImageButton)findViewById(R.id.appwidget_generale_timeMode_helpButton);
        initTimeMode(context);

        //
        // widget: timezone mode
        //
        spinner_timezoneMode = (Spinner)findViewById(R.id.appwidget_timezone_mode);
        if (spinner_timezoneMode != null)
        {
            ArrayAdapter<WidgetSettings.TimezoneMode> spinner_timezoneModeAdapter;
            spinner_timezoneModeAdapter = new ArrayAdapter<WidgetSettings.TimezoneMode>(this, R.layout.layout_listitem_oneline, WidgetSettings.TimezoneMode.values());
            spinner_timezoneModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_timezoneMode.setAdapter(spinner_timezoneModeAdapter);
            spinner_timezoneMode.setOnItemSelectedListener(onTimezoneModeListener);
        }

        //
        // widget: timezone / solartime
        //
        layout_timezone = (LinearLayout)findViewById(R.id.appwidget_timezone_custom_layout);
        label_timezone = (TextView)findViewById(R.id.appwidget_timezone_custom_label);
        spinner_timezone = (Spinner)findViewById(R.id.appwidget_timezone_custom);

        if (label_timezone != null)
        {
            label_timezone.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    triggerTimeZoneActionMode(view);
                }
            });
            label_timezone.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View view)
                {
                    return triggerTimeZoneActionMode(view);
                }
            });
        }

        if (spinner_timezone != null)
        {
            View spinner_timezone_empty = findViewById(R.id.appwidget_timezone_custom_empty);
            spinner_timezone.setEmptyView(spinner_timezone_empty);

            WidgetTimezones.TimeZoneSort sortZonesBy = AppSettings.loadTimeZoneSortPref(context);
            WidgetTimezones.TimeZonesLoadTask loadTask = new WidgetTimezones.TimeZonesLoadTask(context)
            {
                @Override
                protected void onPreExecute()
                {
                    super.onPreExecute();
                    spinner_timezone.setAdapter(new WidgetTimezones.TimeZoneItemAdapter(SuntimesConfigActivity.this, R.layout.layout_listitem_timezone));
                    button_addWidget.setEnabled(false);
                }

                @Override
                protected void onPostExecute(WidgetTimezones.TimeZoneItemAdapter result)
                {
                    spinner_timezone_adapter = result;
                    spinner_timezone.setAdapter(spinner_timezone_adapter);
                    WidgetTimezones.selectTimeZone(spinner_timezone, spinner_timezone_adapter, customTimezoneID);
                    button_addWidget.setEnabled(true);
                }
            };
            loadTask.execute(sortZonesBy);
        }

        layout_solartime = (LinearLayout)findViewById(R.id.appwidget_solartime_layout);
        label_solartime = (TextView)findViewById(R.id.appwidget_solartime_label);
        spinner_solartime = (Spinner)findViewById(R.id.appwidget_solartime);
        if (spinner_solartime != null)
        {
            ArrayAdapter<WidgetSettings.SolarTimeMode> spinner_solartimeAdapter;
            spinner_solartimeAdapter = new ArrayAdapter<WidgetSettings.SolarTimeMode>(this, R.layout.layout_listitem_oneline, WidgetSettings.SolarTimeMode.values());
            spinner_solartimeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_solartime.setAdapter(spinner_solartimeAdapter);
        }

        spinner_timezone_actionMode = new WidgetTimezones.TimeZoneSpinnerSortActionCompat(context, spinner_timezone)
        {
            @Override
            public void onSortTimeZones(WidgetTimezones.TimeZoneItemAdapter result, WidgetTimezones.TimeZoneSort sortMode)
            {
                super.onSortTimeZones(result, sortMode);
                spinner_timezone_adapter = result;
                WidgetTimezones.selectTimeZone(spinner_timezone, spinner_timezone_adapter, customTimezoneID);
            }

            @Override
            public void onSaveSortMode( WidgetTimezones.TimeZoneSort sortMode )
            {
                super.onSaveSortMode(sortMode);
                AppSettings.setTimeZoneSortPref(SuntimesConfigActivity.this, sortMode);
            }

            @Override
            public void onDestroyActionMode(ActionMode mode)
            {
                super.onDestroyActionMode(mode);
                actionMode = null;
            }
        };

        //
        // widget: location
        //
        locationConfig = (LocationConfigView)findViewById(R.id.appwidget_location_config);
        if (locationConfig != null)
        {
            locationConfig.setAutoAllowed(false);
            locationConfig.init(this, false, this.appWidgetId);
        }

        //
        // widget: 1x1 widget mode
        //
        spinner_1x1mode = (Spinner)findViewById(R.id.appwidget_appearance_1x1mode);
        if (spinner_1x1mode != null)
        {
            ArrayAdapter<WidgetSettings.WidgetMode1x1> spinner_1x1ModeAdapter;
            spinner_1x1ModeAdapter = new ArrayAdapter<WidgetSettings.WidgetMode1x1>(this, R.layout.layout_listitem_oneline, WidgetSettings.WidgetMode1x1.values());
            spinner_1x1ModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_1x1mode.setAdapter(spinner_1x1ModeAdapter);
        }

        //
        // widget: title text
        //
        label_titleText = (TextView)findViewById(R.id.appwidget_appearance_titleText_label);
        text_titleText = (EditText)findViewById(R.id.appwidget_appearance_titleText);

        ImageButton button_titleText = (ImageButton)findViewById(R.id.appwidget_appearance_titleText_helpButton);
        if (button_titleText != null)
        {
            button_titleText.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    HelpDialog helpDialog = new HelpDialog();
                    helpDialog.setContent(getString(R.string.help_appearance_title));
                    helpDialog.show(getSupportFragmentManager(), DIALOGTAG_HELP);
                }
            });
        }

        //
        // widget: show title
        //
        checkbox_showTitle = (CheckBox)findViewById(R.id.appwidget_appearance_showTitle);
        if (checkbox_showTitle != null)
        {
            checkbox_showTitle.setOnCheckedChangeListener(onShowTitleListener);
        }

        //
        // widget: allow resize
        //
        checkbox_allowResize = (CheckBox)findViewById(R.id.appwidget_appearance_allowResize);
        if (checkbox_allowResize != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
        {
            disableOptionAllowResize();  // resizable widgets require api16+
        }

        //
        // widget: compare mode
        //
        spinner_compareMode = (Spinner)findViewById(R.id.appwidget_general_compareMode);
        if (spinner_compareMode != null)
        {
            ArrayAdapter<WidgetSettings.CompareMode> spinner_compareModeAdapter;
            spinner_compareModeAdapter = new ArrayAdapter<WidgetSettings.CompareMode>(this, R.layout.layout_listitem_oneline, WidgetSettings.CompareMode.values());
            spinner_compareModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_compareMode.setAdapter(spinner_compareModeAdapter);
        }

        //
        // widget: about button
        //
        Button button_aboutWidget = (Button)findViewById(R.id.about_button);
        if (button_aboutWidget != null)
        {
            button_aboutWidget.setOnClickListener(onAboutButtonClickListener);
        }
    }

    /**
     * @param context
     */
    protected void initTimeMode( Context context )
    {
        if (spinner_timeMode != null)
        {
            ArrayAdapter<WidgetSettings.TimeMode> spinner_timeModeAdapter;
            spinner_timeModeAdapter = new ArrayAdapter<WidgetSettings.TimeMode>(this, R.layout.layout_listitem_oneline, WidgetSettings.TimeMode.values());
            spinner_timeModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_timeMode.setAdapter(spinner_timeModeAdapter);
        }

        if (button_timeModeHelp != null)
        {
            button_timeModeHelp.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    HelpDialog helpDialog = new HelpDialog();
                    helpDialog.setContent(getString(R.string.help_general_timeMode));
                    helpDialog.show(getSupportFragmentManager(), DIALOGTAG_HELP);
                }
            });
        }
    }

    /**
     * @param context
     */
    protected void loadTimeMode(Context context)
    {
        WidgetSettings.TimeMode timeMode = WidgetSettings.loadTimeModePref(context, appWidgetId);
        spinner_timeMode.setSelection(timeMode.ordinal());
    }

    /**
     * @param context
     */
    protected void saveTimeMode(Context context)
    {
        final WidgetSettings.TimeMode[] timeModes = WidgetSettings.TimeMode.values();
        WidgetSettings.TimeMode timeMode = timeModes[ spinner_timeMode.getSelectedItemPosition()];
        WidgetSettings.saveTimeModePref(context, appWidgetId, timeMode);
    }

    private Button button_addWidget;

    private void setActionButtonText( String text )
    {
        if (button_addWidget != null)
        {
            button_addWidget.setText(text);
        }
    }

    private void setTitleTextEnabled( boolean value )
    {
        label_titleText.setEnabled(value);
        text_titleText.setEnabled(value);
    }

    private void setUseSolarTime( boolean value )
    {
        label_solartime.setEnabled(value);
        spinner_solartime.setEnabled(value);
        layout_solartime.setVisibility((value ? View.VISIBLE : View.GONE));
        layout_timezone.setVisibility((value ? View.GONE : View.VISIBLE));
    }

    private void setCustomTimezoneEnabled( boolean value )
    {
        String timezoneID = (value ? customTimezoneID : TimeZone.getDefault().getID());

        if (spinner_timezone_adapter != null)
        {
            spinner_timezone.setSelection(spinner_timezone_adapter.ordinal(timezoneID), true);
        }

        label_timezone.setEnabled(value);
        spinner_timezone.setEnabled(value);
    }

    private boolean triggerTimeZoneActionMode(View view)
    {
        if (actionMode == null)
        {
            actionMode = startSupportActionMode(spinner_timezone_actionMode);
            actionMode.setTitle(getString(R.string.timezone_sort_contextAction));
            return true;
        }
        return false;
    }

    /**
     *
     */
    CheckBox.OnCheckedChangeListener onShowTitleListener = new CheckBox.OnCheckedChangeListener()
    {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
            setTitleTextEnabled(isChecked);
        }
    };

    /**
     * OnItemSelected (TimeZone Mode)
     */
    Spinner.OnItemSelectedListener onTimezoneModeListener = new Spinner.OnItemSelectedListener()
    {
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
        {
            final WidgetSettings.TimezoneMode[] timezoneModes = WidgetSettings.TimezoneMode.values();
            WidgetSettings.TimezoneMode timezoneMode = timezoneModes[ parent.getSelectedItemPosition() ];
            setCustomTimezoneEnabled( (timezoneMode == WidgetSettings.TimezoneMode.CUSTOM_TIMEZONE) );
            setUseSolarTime((timezoneMode == WidgetSettings.TimezoneMode.SOLAR_TIME));
        }

        public void onNothingSelected(AdapterView<?> parent)
        {
        }
    };

    /**
     * OnItemSelected (Action Mode)
     */
    Spinner.OnItemSelectedListener onActionModeListener = new Spinner.OnItemSelectedListener()
    {
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
        {
            final WidgetSettings.ActionMode[] actionModes = WidgetSettings.ActionMode.values();
            WidgetSettings.ActionMode actionMode = actionModes[ parent.getSelectedItemPosition() ];

            View launchActionView = findViewById(R.id.applayout_action_launch);
            if (launchActionView != null)
            {
                switch (actionMode)
                {
                    case ONTAP_LAUNCH_ACTIVITY:
                        launchActionView.setVisibility(View.VISIBLE);
                        break;

                    case ONTAP_DONOTHING:
                    default:
                        launchActionView.setVisibility(View.GONE);
                        break;
                }
            }
        }

        public void onNothingSelected(AdapterView<?> parent)
        {
        }
    };

    /**
     * Save UI state to settings (appearance group).
     * @param context the android application context
     */
    protected void saveAppearanceSettings(Context context)
    {
        // save: widgetmode_1x1
        final WidgetSettings.WidgetMode1x1[] modes = WidgetSettings.WidgetMode1x1.values();
        WidgetSettings.WidgetMode1x1 mode = modes[ spinner_1x1mode.getSelectedItemPosition() ];
        WidgetSettings.save1x1ModePref(context, appWidgetId, mode);
        //Log.d("DEBUG", "Saved mode: " + mode.name());

        // save: theme
        final ThemeDescriptor[] themes = WidgetThemes.values();
        ThemeDescriptor theme = themes[ spinner_theme.getSelectedItemPosition() ];
        WidgetSettings.saveThemePref(context, appWidgetId, theme.name());
        //Log.d("DEBUG", "Saved theme: " + theme.name());

        // save: allow resize
        boolean allowResize = checkbox_allowResize.isChecked();
        WidgetSettings.saveAllowResizePref(context, appWidgetId, allowResize);

        // save: show title
        boolean showTitle = checkbox_showTitle.isChecked();
        WidgetSettings.saveShowTitlePref(context, appWidgetId, showTitle);

        // save:: title text
        String titleText = text_titleText.getText().toString().trim();
        WidgetSettings.saveTitleTextPref(context, appWidgetId, titleText);
    }

    /**
     * Load settings into UI state (appearance group).
     * @param context the android application context
     */
    protected void loadAppearanceSettings(Context context)
    {
        // load: widgetmode_1x1
        WidgetSettings.WidgetMode1x1 mode1x1 = WidgetSettings.load1x1ModePref(context, appWidgetId);
        spinner_1x1mode.setSelection(mode1x1.ordinal());

        // load: theme
        SuntimesTheme theme = WidgetSettings.loadThemePref(context, appWidgetId);
        ThemeDescriptor themeDescriptor = WidgetThemes.valueOf(theme.themeName());
        spinner_theme.setSelection(themeDescriptor.ordinal(WidgetThemes.values()));

        // load: allow resize
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            boolean allowResize = WidgetSettings.loadAllowResizePref(context, appWidgetId);
            checkbox_allowResize.setChecked(allowResize);
        } else {
            disableOptionAllowResize();
        }

        // load: show title
        boolean showTitle = WidgetSettings.loadShowTitlePref(context, appWidgetId);
        checkbox_showTitle.setChecked(showTitle);
        setTitleTextEnabled(showTitle);

        // load: title text
        String titleText = WidgetSettings.loadTitleTextPref(context, appWidgetId);
        text_titleText.setText(titleText);
    }

    /**
     * Save UI state to settings (general group).
     * @param context the android application context
     */
    protected void saveGeneralSettings(Context context)
    {
        // save: calculator mode
        final SuntimesCalculatorDescriptor[] calculators = SuntimesCalculatorDescriptor.values();
        SuntimesCalculatorDescriptor calculator = calculators[ spinner_calculatorMode.getSelectedItemPosition() ];
        WidgetSettings.saveCalculatorModePref(context, appWidgetId, calculator);

        // save: compare mode
        final WidgetSettings.CompareMode[] compareModes = WidgetSettings.CompareMode.values();
        WidgetSettings.CompareMode compareMode = compareModes[ spinner_compareMode.getSelectedItemPosition()];
        WidgetSettings.saveCompareModePref(context, appWidgetId, compareMode);

        // save: time mode
        saveTimeMode(context);
    }

    /**
     * Load settings into UI state (general group).
     * @param context the android application context
     */
    protected void loadGeneralSettings(Context context)
    {
        // load: calculator mode
        SuntimesCalculatorDescriptor calculatorMode = WidgetSettings.loadCalculatorModePref(context, appWidgetId);
        spinner_calculatorMode.setSelection(calculatorMode.ordinal());

        // load: compare mode
        WidgetSettings.CompareMode compareMode = WidgetSettings.loadCompareModePref(context, appWidgetId);
        spinner_compareMode.setSelection(compareMode.ordinal());

        // load: time mode
        loadTimeMode(context);
    }

    /**
     * Save UI state to settings (timezone group).
     * @param context the android application context
     */
    protected void saveTimezoneSettings(Context context)
    {
        // save: timezone mode
        final WidgetSettings.TimezoneMode[] timezoneModes = WidgetSettings.TimezoneMode.values();
        WidgetSettings.TimezoneMode timezoneMode = timezoneModes[ spinner_timezoneMode.getSelectedItemPosition() ];
        WidgetSettings.saveTimezoneModePref(context, appWidgetId, timezoneMode);

        // save: custom timezone
        WidgetTimezones.TimeZoneItem customTimezone = (WidgetTimezones.TimeZoneItem)spinner_timezone.getSelectedItem();
        if (customTimezone != null)
        {
            WidgetSettings.saveTimezonePref(context, appWidgetId, customTimezone.getID());
        } else {
            Log.e("saveTimezoneSettings", "Failed to save timezone; none selected (was null). The timezone selector may not have been fully loaded..");
        }

        // save: solar timemode
        WidgetSettings.SolarTimeMode[] solarTimeModes = WidgetSettings.SolarTimeMode.values();
        WidgetSettings.SolarTimeMode solarTimeMode = solarTimeModes[spinner_solartime.getSelectedItemPosition()];
        WidgetSettings.saveSolarTimeModePref(context, appWidgetId, solarTimeMode);
    }

    /**
     * Load settings into UI state (timezone group).
     * @param context the android application context
     */
    protected void loadTimezoneSettings(Context context)
    {
        WidgetSettings.TimezoneMode timezoneMode = WidgetSettings.loadTimezoneModePref(context, appWidgetId);
        spinner_timezoneMode.setSelection(timezoneMode.ordinal());

        WidgetSettings.SolarTimeMode solartimeMode = WidgetSettings.loadSolarTimeModePref(context, appWidgetId);
        spinner_solartime.setSelection(solartimeMode.ordinal());

        setCustomTimezoneEnabled(timezoneMode == WidgetSettings.TimezoneMode.CUSTOM_TIMEZONE);
        setUseSolarTime(timezoneMode == WidgetSettings.TimezoneMode.SOLAR_TIME);

        customTimezoneID = WidgetSettings.loadTimezonePref(context, appWidgetId);
        WidgetTimezones.selectTimeZone(spinner_timezone, spinner_timezone_adapter, customTimezoneID);
    }

    /**
     * Save UI state to settings (action group).
     * @param context the android application context
     */
    protected void saveActionSettings(Context context)
    {
        // save: action mode
        WidgetSettings.ActionMode actionMode = (WidgetSettings.ActionMode)spinner_onTap.getSelectedItem();
        WidgetSettings.saveActionModePref(context, appWidgetId, actionMode);

        // save: launch activity
        String launchString = text_launchActivity.getText().toString();
        WidgetSettings.saveActionLaunchPref(context, appWidgetId, launchString);
    }

    /**
     * Load settings into UI state (action group).
     * @param context the android application context
     */
    protected void loadActionSettings(Context context)
    {
        // load: action mode
        WidgetSettings.ActionMode actionMode = WidgetSettings.loadActionModePref(context, appWidgetId);
        spinner_onTap.setSelection(actionMode.ordinal(supportedActionModes()));

        // load: launch activity
        String launchString = WidgetSettings.loadActionLaunchPref(context, appWidgetId);
        text_launchActivity.setText(launchString);
    }

    /**
     * Click handler executed when the "Add Widget" button is pressed.
     */
    View.OnClickListener onAddButtonClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            addWidget();
        }
    };

    protected void addWidget()
    {
        boolean hasValidInput = locationConfig.validateInput();  // todo: && validate other potentially troublesome input values
        if (hasValidInput)
        {
            locationConfig.setMode(LocationConfigView.LocationViewMode.MODE_CUSTOM_SELECT);
            locationConfig.populateLocationList();  // triggers 'add place'

            final Context context = SuntimesConfigActivity.this;
            saveSettings(context);
            updateWidget(context);

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    }

    protected void updateWidget( Context context )
    {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        SuntimesWidget.updateAppWidget(context, appWidgetManager, appWidgetId);
    }

    /**
     * Click handler executed when the "About" button is pressed.
     */
    View.OnClickListener onAboutButtonClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            AboutDialog aboutDialog = new AboutDialog();
            aboutDialog.show(getSupportFragmentManager(), DIALOGTAG_ABOUT);
        }
    };

    /**
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        locationConfig.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     *
     */
    protected void disableOptionAllowResize()
    {
        if (checkbox_allowResize != null)
        {
            checkbox_allowResize.setChecked(false);
            checkbox_allowResize.setEnabled(false);
        }
    }

    /**
     *
     */
    protected void hideOptionCompareAgainst()
    {
        View layout_compareMode = findViewById(R.id.appwidget_general_compareMode_layout);
        if (layout_compareMode != null)
        {
            layout_compareMode.setVisibility(View.GONE);
        }
    }

    /**
     *
     */
    protected void hideOptionLayoutMode()
    {
        View layout_1x1mode = findViewById(R.id.appwidget_appearance_1x1mode_layout);
        if (layout_1x1mode != null)
        {
            layout_1x1mode.setVisibility(View.GONE);
        }
    }

    /**
     * @param text activity title text
     */
    protected void setConfigActivityTitle(String text)
    {
        TextView activityTitle = (TextView) findViewById(R.id.activity_title);
        if (activityTitle != null)
        {
            activityTitle.setText(text);
        }
    }
}
