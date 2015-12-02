package edu.uta.ucs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

/**
 * This class is used to store schedules.
 *
 * <p>Contains 3 objects</p>
 *
 * <li>String name, the name of the schedule as selected by user or generated by program</li>
 * <li>int semesterNumber, the number of the semester this schedule is built for</li>
 * <li>ArrayList<Section> selectedSection, list of sections in schedule</></li>
 */
public class Schedule {

    public static final String SCHEDULE_NAMES = "SCHEDULE_NAMES";
    public static final String SCHEDULE_SAVEFILE = "SCHEDULE_SAVEFILE";

    private static final String ACTION_VERIFY_SCHEDULE = "ACTION_VERIFY_SCHEDULE";

    private String name = null;
    private int semesterNumber = 0;
    private int scheduleID = 0;
    private ArrayList<Section> selectedSections;
    private ArrayList<Section> selectedBlockOutTimes;

    /**
     * Constructs a new Schedule object with the provided information
     *
     * @param name String name for this schedule.
     * @param semesterNumber Semester Number this schedule is built for. Must match UTA semester numbers
     * @param sectionArrayList Arraylist of sections to show in this schedule.
     */
    Schedule(String name, int semesterNumber, ArrayList<Section> sectionArrayList){
        this.name = name;
        this.semesterNumber = semesterNumber;
        this.selectedSections = sectionArrayList;
    }

    Schedule(String name, int semesterNumber, ArrayList<Section> sectionArrayList, ArrayList<Section> blockOutTimesList){
        this.name = name;
        this.semesterNumber = semesterNumber;
        this.selectedSections = sectionArrayList;
        this.selectedBlockOutTimes = blockOutTimesList;
    }

    /**
     * Constructs a new Schedule object from a JSONObject
     *
     * @param scheduleJSON JSON Object must have the following keys present:
     *                   <ul>
     *                   <li>"ScheduleName" - String, name of the schedule to be built</li>
     *                   <li>"ScheduleSemester" - int, Semester Number this schedule is built for.</li>
     *                   <li>"ScheduleCourses" - JSONArray, Arraylist of courses this schedule will have. Each course should only have one section in it.
     *                     See {@link Course#Course(JSONObject)} for required keys in the JSONObjects this JSONArray should have in it. </li>
     *                   <ul/>
     * @throws JSONException
     */
    Schedule(JSONObject scheduleJSON) throws JSONException {

        name = scheduleJSON.getString("ScheduleName");
        semesterNumber = scheduleJSON.getInt("ScheduleSemester");
        if(scheduleJSON.has("ScheduleID")){
            scheduleID = scheduleJSON.getInt("ScheduleID");
        }
        Log.i("Schedule Course", scheduleJSON.getString("ScheduleCourses"));

        JSONArray scheduleCoursesJSONArray = scheduleJSON.getJSONArray("ScheduleCourses");
        ArrayList<Course> semesterCourses = Course.buildCourseList(scheduleCoursesJSONArray);
        selectedSections = new ArrayList<>(semesterCourses.size());
        for (Course course : semesterCourses){
            selectedSections.addAll(course.getSectionList());
        }

        if(scheduleJSON.has("BlockOutTimes")){
            JSONArray blockOutTimesJSONArray = scheduleJSON.getJSONArray("BlockOutTimes");
            ArrayList<Course> blockOutTimes = Course.buildCourseList(blockOutTimesJSONArray);
            for (Course course : blockOutTimes)
            {
                selectedSections.addAll(course.getSectionList());
            }
        }




    }

    public String getName() {
        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    /**
     * Obtains all sections in the schedule
     * @return ArrayList<Section>
     */
    public ArrayList<Section> getSelectedSections() {
        return selectedSections;
    }

    /**
     * Constucts a JSONObject with the same parameters expected by this object's JSON constructor.
     *
     *
     * @return JSONObject
     * @throws JSONException
     * @see #Schedule(JSONObject)
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject result = new JSONObject();

        result.put("ScheduleName", name);
        result.put("ScheduleSemester", getSemesterNumber());
        if(scheduleID != 0){
            result.put("ScheduleID", scheduleID);
        }

        ArrayList<JSONObject> selectedSectionsString = new ArrayList<>(selectedSections.size());

        for (Section section : selectedSections){
            selectedSectionsString.add(section.getSourceCourse().toJSON(section));
        }
        JSONArray selectedSectionsJSONArray = new JSONArray(selectedSectionsString);
        result.put("ScheduleCourses", selectedSectionsJSONArray);
        if (selectedBlockOutTimes != null) {
            ArrayList<JSONObject> selectedBlockOutTimesString = new ArrayList<>(selectedBlockOutTimes.size()); //added
            for (Section blockOuTime : selectedBlockOutTimes) { //added
                selectedBlockOutTimesString.add(blockOuTime.getSourceCourse().toJSON(blockOuTime));
            }
            JSONArray selectedBlockOutTimesJSONArray = new JSONArray(selectedBlockOutTimesString);
            result.put("BlockOutTimes", selectedBlockOutTimesJSONArray);
        }






        return result;
    }


    /**
     * Builds an ArrayList of Schedules based on a JSONArray
     *
     * @param jsonSchedules JSONArray of schedules
     * @return ArrayList<Schedules>
     * @throws JSONException
     */
    public static ArrayList<Schedule> buildScheduleList(JSONArray jsonSchedules) throws JSONException {

        ArrayList<Schedule> scheduleList = new ArrayList<>(jsonSchedules.length());

        for(int index = jsonSchedules.length(); index != 0;index--){
            JSONObject scheduleJSON;
            try {
                scheduleJSON = jsonSchedules.getJSONObject(index - 1);
            }
            catch (JSONException e){
                Log.i("New Schedule JSON", "JSON Construction failed. Attempting to construct JSON from String");
                String courseString = jsonSchedules.getString(index - 1);
                scheduleJSON = new JSONObject(courseString);
            }

            Log.i("New Schedule JSON", "Adding to ArrayList: " + scheduleJSON.toString());
            Schedule parsedSchedule = new Schedule(scheduleJSON);
            scheduleList.add(parsedSchedule);
            if(scheduleJSON.has("ScheduleID")){
                parsedSchedule.scheduleID = scheduleJSON.getInt("ScheduleID");
            }
        }
        Collections.reverse(scheduleList);

        return scheduleList;
    }

    public String fileName(){
        return Schedule.SCHEDULE_NAMES + "_" + name;
    }

    /**
     * Saves all schedules in the list of schedules provided to a sharedPreferance file. This will overwrite all schedules currently in that file.
     *
     * @param context Context to save with. Usually will be the calling class followed by ".this"
     *                <br>EX: MainActivity.this
     * @param schedulesToSave ArrayList of schedules
     */
    public static void saveSchedulesToFile(Context context, ArrayList<Schedule> schedulesToSave){

        SharedPreferences.Editor scheduleEditor;
        scheduleEditor = context.getSharedPreferences(Schedule.SCHEDULE_SAVEFILE, Context.MODE_PRIVATE).edit();
        scheduleEditor.clear();

        //noinspection MismatchedQueryAndUpdateOfCollection
        ArrayList<JSONObject> savedSchedules = new ArrayList<>(schedulesToSave.size());
        ArrayList<String> scheduleNames = new ArrayList<>(schedulesToSave.size());

        for (Schedule schedule : schedulesToSave){
            scheduleNames.add(schedule.getName());
            try {
                savedSchedules.add(schedule.toJSON());
                scheduleEditor.putString(Schedule.SCHEDULE_NAMES + "_" + schedule.getName(), schedule.toJSON().toString());
                Log.i("Saving schedule name",schedule.getName());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        HashSet<String> scheduleNameSet = new HashSet<>(scheduleNames);
        scheduleEditor.putStringSet(Schedule.SCHEDULE_NAMES, scheduleNameSet);
        scheduleEditor.apply();

    }

    /**
     *
     * @param schedule The schedule to save to file
     */
    public static void saveScheduleToFile(Schedule schedule){

        Context context = UserData.getContext();

        SharedPreferences.Editor editor = context.getSharedPreferences(Schedule.SCHEDULE_SAVEFILE, Context.MODE_PRIVATE).edit();

        String scheduleToString;
        try {
            scheduleToString = schedule.toJSON().toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        String scheduleName = schedule.fileName();
        Log.i("Schedule to Save", "Name: " + scheduleName + " JSON: " + scheduleToString);

        editor.putString(scheduleName, scheduleToString);
        editor.apply();

    }



    /**
     * Loads all schedules from the Schedule Savefile into an ArrayList.
     * @return An arraylist of all schedules which could be parsed from the shared preference file 'SCHEDULE_SAVEFILE'
     */
    public static ArrayList<Schedule> loadSchedulesFromFile(){

        SharedPreferences scheduleFile = UserData.getContext().getSharedPreferences(Schedule.SCHEDULE_SAVEFILE, Context.MODE_PRIVATE);
        Map<String, ?> schedulesOnFile = scheduleFile.getAll();

        ArrayList<Schedule> scheduleArrayList = new ArrayList<>(schedulesOnFile.size());
        for(String key : schedulesOnFile.keySet()){
            if (key != SCHEDULE_NAMES){
                Log.i("Load Schedules", "Loading Schedule Name: " + key);
                String scheduleBody = schedulesOnFile.get(key).toString();
                Log.i("Load Schedules", "Loading Schedule body: " + scheduleBody);

                try {
                    JSONObject scheduleJSON = new JSONObject(scheduleBody);
                    Schedule schedule = new Schedule(scheduleJSON);
                    Log.i("Load Schedules", "Schedule JSON" + schedule.toJSON().toString());
                    scheduleArrayList.add(schedule);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }

        return scheduleArrayList;
    }

    public static void clearSchedulesFromFile(){
        Context context = UserData.getContext();

        SharedPreferences.Editor editor = context.getSharedPreferences(Schedule.SCHEDULE_SAVEFILE, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.commit();
    }

    /**
     * Removes the schedule with name equal to this schedule
     *
     * Future suggestion: Perhaps it should only remove the schedule if the contents remain the same.
     */
    public static void removeScheduleFromFile(Schedule schedule){

        Context context = UserData.getContext();

        SharedPreferences reader = context.getSharedPreferences(Schedule.SCHEDULE_SAVEFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = context.getSharedPreferences(Schedule.SCHEDULE_SAVEFILE, Context.MODE_PRIVATE).edit();
        Map<String, ?> schedules = reader.getAll();

        String scheduleFileName = schedule.fileName();


        if(schedules.containsKey(scheduleFileName)){
            editor.remove(scheduleFileName);
            editor.apply();
        }
    }

    /**
     * Creates a query to check if the statuses of sections in this schedule have changed.
     * @param context The context from which this method was called. Used to ensure HTTPService can function without errors.
     */
    public void verifySchedule(Context context){

        Log.i("Verify Schedule", "About to attempt verify schedule");

        StringBuilder semesterParam = new StringBuilder();
        StringBuilder classNumberParam = new StringBuilder(UserData.getContext().getString(R.string.validate_courses_param_sections));

        for (Section section : selectedSections){

            classNumberParam.append(section.getSectionID()).append(",");
        }

        String courseNumberParamFinal = classNumberParam.length() > 0 ? classNumberParam.substring( 0, classNumberParam.length() - 1 ): "";

        String urlFinal = UserData.getContext().getString(R.string.validate_courses_base) + UserData.getContext().getString(R.string.validate_courses_param_semester) + this.getSemesterNumber() + courseNumberParamFinal;

        HTTPService.FetchURL(urlFinal, ACTION_VERIFY_SCHEDULE, context);

    }

    /**
     * Initial schedule generator call. Will initialize the recursive version of schedule generator to execute logic.
     *
     * @param courseArrayList Arraylist with all courses this schedule can pick from.
     * @param blockOutTimesList Arraylist of Block-Out Times this schedule should avoid conflicts with.
     * @return a schedule built with the selected courses
     * @throws NoSchedulesPossibleException
     */
    public static Schedule scheduleFactory(ArrayList<Course> courseArrayList, ArrayList<Section> blockOutTimesList, int semesterNumber) throws NoSchedulesPossibleException{
        /*
        SharedPreferences preferences = UserData.getContext().getSharedPreferences("C", Context.MODE_PRIVATE);
        SharedPreferences.Editor preferencesEditor = preferences.edit();
        //preferencesEditor.clear();
        preferencesEditor.putString(HTTPService.SPOOFED_RESPONSE, spoofData).commit();*/


        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(UserData.getContext());

        boolean allowNonOpenClassesSetting = settings.getBoolean(UserData.getContext().getResources().getString(R.string.pref_key_allow_nonopen_classes), false);

        ArrayList<Section> selectedSections = scheduleBuilder(0, courseArrayList, new ArrayList<Section>(), blockOutTimesList, allowNonOpenClassesSetting);
        return new Schedule("Generated Schedule", semesterNumber, selectedSections, blockOutTimesList); //Todd added new constructor with blockouts included
    }

    /**
     * Recursively builds schedules using the provided information.
     * It will get the course at the provided index, shuffle the arraylist of courses to ensure any two executions of the generator will be unique, and select a section.
     * If the allowNonOpenClasses boolean is set to false the function will only allow selection of a section if the section status is {@link ClassStatus#OPEN}, otherwise it will allow any section.
     * It will then compare the selected section it to all sections in the already selected sections ArrayList for conflicts.
     * It will then compare the selected section it to all sections in the block-out times ArrayList for conflicts.
     * If no conflics are detected the section is added to the already selected sections Arraylist, and the function is recursively called with an incremented index.
     * If a conflict is detected the next section in the course is selected.
     * If all sections in the course have been tested and all of them conflict with the selected sections arraylist or the blockout times list the function will throw a NoSchedulesPossible error.
     *
     * If the recursive call throws a NoSchedulesPossible error the last selected section is removed from the selected sections list and the function selects the next section on the current index.
     *
     * @param index index of course in courseArrayList which will be attempted to be added to the schedule
     * @param courseArrayList Arraylist of courses to select sections from.
     * @param alreadySelectedSections Arraylist of sections which have already been considered for this schedule.
     *                                Selected section is not permitted to conflict with this.
     * @param blockOutTimesList Arraylist of block-out times which a user had defined.
     *                                Selected section is not permitted to conflict with this.
     * @param allowNonOpenClasses Boolean toggle which allows classes to be added if they are not open
     * @return Arraylist of section which do not conflict with each other or the provided block-out times.
     * @throws NoSchedulesPossibleException if no section could be selected without conflicts.
     */
    public static ArrayList<Section> scheduleBuilder(int index, ArrayList<Course> courseArrayList, ArrayList<Section> alreadySelectedSections, ArrayList<Section> blockOutTimesList, boolean allowNonOpenClasses) throws NoSchedulesPossibleException{

        // Create an error to throw if no schedule can be built. It will buffer all errors so that
        NoSchedulesPossibleException scheduleConflict = new NoSchedulesPossibleException("");
        Log.i("schedule Factory", "Loop Counter:" + ((Integer) index).toString());
        if (index == courseArrayList.size()){
            return alreadySelectedSections;
        }
        Course course = courseArrayList.get(index);
        ArrayList<Section> possibleSections = new ArrayList<>(course.getSectionList().size());
        possibleSections.addAll(course.getSectionList());
        Collections.shuffle(possibleSections);

        for (Section section : possibleSections){

            if(!allowNonOpenClasses)
                if(section.getStatus() != ClassStatus.OPEN)
                    continue;

            boolean conflictDetected = false;

            try {
                for (Section sectionToCompare : blockOutTimesList) {
                    if (section.conflictsWith(sectionToCompare)) {
                        throw new NoSchedulesPossibleException(section, sectionToCompare);
                    }
                }
            } catch (NoSchedulesPossibleException innerException){
                Log.w("Schedule Generator", "Conflict With Block-Out time detected");
                scheduleConflict.addConflict(innerException);
                conflictDetected = true;
            }

            try {
                for (Section sectionToCompare : alreadySelectedSections) {
                    if (section.conflictsWith(sectionToCompare)) {
                        throw new NoSchedulesPossibleException(section, sectionToCompare);
                    }
                }
            } catch (NoSchedulesPossibleException innerException){
                Log.w("Schedule Generator", "Conflict With already selected sections detected");
                scheduleConflict.addConflict(innerException);
                conflictDetected = true;
            }

            if(!conflictDetected){
                Log.i("Adding Section to List", section.toJSON().toString());
                alreadySelectedSections.add(section); //Add the class section to this array if there are no conflicts with the blockout times

                try{
                    return scheduleBuilder(index + 1, courseArrayList, alreadySelectedSections, blockOutTimesList, allowNonOpenClasses);
                } catch (NoSchedulesPossibleException exception){
                    exception.printStackTrace();
                    alreadySelectedSections.remove(index);
                    scheduleConflict = exception;
                }

            }

        }throw scheduleConflict;

    }


    /**
     * Initial schedule generator call. Will initialize the recursive version of schedule generator to execute logic.
     *
     * @param courseArrayList Arraylist with all courses this schedule can pick from.
     * @return a schedule constructed with these courses
     * @throws NoSchedulesPossibleException
     */
    public static Schedule scheduleFactoryIgnoreConflicts(ArrayList<Course> courseArrayList, int semesterNumber) throws NoSchedulesPossibleException{

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(UserData.getContext());
        boolean allowNonOpenClassesSetting = settings.getBoolean(UserData.getContext().getResources().getString(R.string.pref_key_allow_nonopen_classes), false);

        ArrayList<Section> selectedSections = scheduleBuilderIgnoreConflicts(0, courseArrayList, new ArrayList<Section>(), allowNonOpenClassesSetting);
        return new Schedule("Generated Schedule", semesterNumber, selectedSections);
    }

    /**
     * Recursively builds schedules using the provided information.
     * It will get the course at the provided index, shuffle the arraylist of courses to ensure any two executions of the generator will be unique, and select a section.
     * If the allowNonOpenClasses boolean is set to false the function will only allow selection of a section if the section status is {@link ClassStatus#OPEN}, otherwise it will allow any section.
     * If all sections in the course have been tested and none of the sections is suitable the function will throw a NoSchedulesPossible error.
     *
     * If the recursive call throws a NoSchedulesPossible error the last selected section is removed from the selected sections list and the function selects the next section on the current index.
     *
     * @param index index of course in courseArrayList which will be attempted to be added to the schedule
     * @param courseArrayList Arraylist of courses to select sections from.
     * @param alreadySelectedSections Arraylist of sections which have already been considered for this schedule.
     *                                Selected section is not permitted to conflict with this.
     * @param allowNonOpenClasses Boolean toggle which allows classes to be added if they are not open
     * @return Arraylist of section which do not conflict with each other or the provided block-out times.
     * @throws NoSchedulesPossibleException if no section could be selected without conflicts.
     */
    public static ArrayList<Section> scheduleBuilderIgnoreConflicts(int index, ArrayList<Course> courseArrayList,ArrayList<Section> alreadySelectedSections, boolean allowNonOpenClasses) throws NoSchedulesPossibleException {

        Log.i("schedule Factory", "Loop Counter:" + ((Integer) index).toString());

        if (index == courseArrayList.size()){
            return alreadySelectedSections;
        }

        ArrayList<Section>  possibleSections = courseArrayList.get(index).getSectionList();
        Collections.shuffle(possibleSections);
        for (Section section : possibleSections) {

            if (!allowNonOpenClasses)
                if (section.getStatus() != ClassStatus.OPEN) {
                    continue;
                }

            alreadySelectedSections.add(section);
            return scheduleBuilderIgnoreConflicts(index + 1, courseArrayList, alreadySelectedSections, allowNonOpenClasses);

        } throw new NoSchedulesPossibleException("No Open Classes found for course: "+ courseArrayList.get(index).getCourseDescription());

    }

    public int getSemesterNumber() {
        return semesterNumber;
    }

}

/**
 * Exception for schedule generation. It will hold onto any error passed to it in string form to show the user after generation is complete.
 */
class NoSchedulesPossibleException extends Exception {

    StringBuilder message;

    /**
     * Constructs a new exception with the message initialized to the string passed to it.
     */
    public NoSchedulesPossibleException(String message) {
        this.message = new StringBuilder(message);
    }

    /**
     * Constructs a new exception with the message initialized to match the content of the exception passed to it.
     */
    @SuppressWarnings("unused")
    public NoSchedulesPossibleException(NoSchedulesPossibleException innerError) {
        this.message = new StringBuilder(innerError.message.toString());
    }

    /**
     * Constructs a new exception and sets the content of the message carried in it to a string describing the conflicting sections passed to the constructor.
     */
    public NoSchedulesPossibleException(Section firstSection, Section secondSection){
        this.message = new StringBuilder("Conflict between " + firstSection.getDescription() + " and " + secondSection.getDescription());
    }

    /**
     * Adds the content of the exception passed to the current message.
     * Adds a new line if the current message has no previous content.
     */
    public void addConflict(NoSchedulesPossibleException innerError){

        this.message.append((this.message.length() == 0) ? "" : "\n").append(innerError.message.toString());

        Log.w("Conflict Added", this.message.toString());
    }

    /**
     * Adds the a string describing the conflicting sections passed to the method.
     * Adds a new line if the current message has no previous content.
     */
    @SuppressWarnings("unused")
    public void addConflict(Section firstSection, Section secondSection){
        this.message.append((this.message.length() == 0) ? "" : "\n").append("Conflict between ").append(firstSection.getDescription()).append(" and ").append(secondSection.getDescription());

        Log.w("Conflict Added", this.message.toString());
    }

    public String printConflict(){
        Log.e("Cannot Generate", message.toString());
        return message.toString();
    }

}
