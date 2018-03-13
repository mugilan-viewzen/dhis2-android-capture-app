package com.dhis2.data.metadata;

import android.support.annotation.NonNull;

import com.squareup.sqlbrite2.BriteDatabase;

import org.hisp.dhis.android.core.category.CategoryComboModel;
import org.hisp.dhis.android.core.category.CategoryOptionComboModel;
import org.hisp.dhis.android.core.category.CategoryOptionModel;
import org.hisp.dhis.android.core.dataelement.DataElementModel;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.enrollment.EnrollmentModel;
import org.hisp.dhis.android.core.event.EventModel;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitModel;
import org.hisp.dhis.android.core.program.ProgramModel;
import org.hisp.dhis.android.core.program.ProgramStageDataElementModel;
import org.hisp.dhis.android.core.program.ProgramStageModel;
import org.hisp.dhis.android.core.program.ProgramTrackedEntityAttributeModel;
import org.hisp.dhis.android.core.relationship.RelationshipTypeModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValueModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValueModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstanceModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityModel;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Observable;


/**
 * Created by ppajuelo on 04/12/2017.
 */

public class MetadataRepositoryImpl implements MetadataRepository {

    private static final String SELECT_PROGRMAS_TO_ENROLL = String.format(
            "SELECT * FROM %s WHERE %s.%s = ?",
            ProgramModel.TABLE, ProgramModel.TABLE, ProgramModel.Columns.TRACKED_ENTITY
    );

    private static final String SELECT_TEI_ENROLLMENTS = String.format(
            "SELECT * FROM %s WHERE %s.%s =",
            EnrollmentModel.TABLE,
            EnrollmentModel.TABLE, EnrollmentModel.Columns.TRACKED_ENTITY_INSTANCE);

    private static final String SELECT_ENROLLMENT_LAST_EVENT = String.format(
            "SELECT %s.* FROM %s JOIN %s ON %s.%s = %s.%s WHERE %s.%s = ? ORDER BY %s.%s DESC LIMIT 1",
            EventModel.TABLE, EventModel.TABLE, EnrollmentModel.TABLE, EnrollmentModel.TABLE, EnrollmentModel.Columns.UID, EventModel.TABLE, EventModel.Columns.ENROLLMENT_UID,
            EnrollmentModel.TABLE, EnrollmentModel.Columns.UID, EventModel.TABLE, EventModel.Columns.EVENT_DATE
    );
    private Set<String> SELECT_ENROLLMENT_LAST_EVENT_TABLES = new HashSet<>(Arrays.asList(EventModel.TABLE, EnrollmentModel.TABLE));


    private final String PROGRAM_LIST_QUERY = String.format("SELECT * FROM %s WHERE ",
            ProgramModel.TABLE);

    private final String ACTIVE_TEI_PROGRAMS = String.format(
            " SELECT %s.* FROM %s " +
                    "JOIN %s ON %s.%s = %s.%s " +
                    "WHERE %s.%s = ?",
            ProgramModel.TABLE,
            ProgramModel.TABLE,
            EnrollmentModel.TABLE, EnrollmentModel.TABLE, EnrollmentModel.Columns.PROGRAM, ProgramModel.TABLE, ProgramModel.Columns.UID,
            EnrollmentModel.TABLE, EnrollmentModel.Columns.TRACKED_ENTITY_INSTANCE);

    private Set<String> ACTIVE_TEI_PROGRAMS_TABLES = new HashSet<>(Arrays.asList(ProgramModel.TABLE, EnrollmentModel.TABLE));


    private final String PROGRAM_LIST_ALL_QUERY = String.format("SELECT * FROM %s WHERE %s.%s = ",
            ProgramModel.TABLE, ProgramModel.TABLE, ProgramModel.Columns.UID);

    private final String TRACKED_ENTITY_QUERY = String.format("SELECT * FROM %s WHERE %s.%s = ",
            TrackedEntityModel.TABLE, TrackedEntityModel.TABLE, TrackedEntityModel.Columns.UID);

    private final String TRACKED_ENTITY_INSTANCE_QUERY = String.format("SELECT * FROM %s WHERE %s.%s = ",
            TrackedEntityInstanceModel.TABLE, TrackedEntityInstanceModel.TABLE, TrackedEntityInstanceModel.Columns.UID);

    private final String ORG_UNIT_QUERY = String.format("SELECT * FROM %s WHERE %s.%s = ",
            OrganisationUnitModel.TABLE, OrganisationUnitModel.TABLE, OrganisationUnitModel.Columns.UID);

    private final String TEI_ORG_UNIT_QUERY = String.format(
            "SELECT * FROM %s " +
                    "JOIN %s ON %s.%s = %s.%s " +
                    "WHERE  %s.%s = ?",
            OrganisationUnitModel.TABLE,
            TrackedEntityInstanceModel.TABLE, TrackedEntityInstanceModel.TABLE, TrackedEntityInstanceModel.Columns.ORGANISATION_UNIT, OrganisationUnitModel.TABLE, OrganisationUnitModel.Columns.UID,
            TrackedEntityInstanceModel.TABLE, TrackedEntityInstanceModel.Columns.UID);


    private Set<String> TEI_ORG_UNIT_TABLES = new HashSet<>(Arrays.asList(OrganisationUnitModel.TABLE, TrackedEntityInstanceModel.TABLE));

    private final String ORG_UNIT_DATE_QUERY = String.format(
            "SELECT * FROM %s " +
                    "WHERE %s.%s < ? AND %s.%s > ?",
            OrganisationUnitModel.TABLE,
            OrganisationUnitModel.TABLE, OrganisationUnitModel.Columns.OPENING_DATE, OrganisationUnitModel.TABLE, OrganisationUnitModel.Columns.CLOSED_DATE
    );

    private final String PROGRAM_TRACKED_ENTITY_ATTRIBUTES_QUERY = String.format("SELECT * FROM %s WHERE %s.%s = ",
            ProgramTrackedEntityAttributeModel.TABLE, ProgramTrackedEntityAttributeModel.TABLE, ProgramTrackedEntityAttributeModel.Columns.PROGRAM);

    private final String PROGRAM_TRACKED_ENTITY_ATTRIBUTES_NO_PROGRAM_QUERY = String.format(
            "SELECT %s.* FROM %s " +
                    "JOIN %s ON %s.%s = %s.%s " +
                    "WHERE %s.%s = '1' GROUP BY %s.%s",
            ProgramTrackedEntityAttributeModel.TABLE, ProgramTrackedEntityAttributeModel.TABLE,
            TrackedEntityAttributeModel.TABLE, TrackedEntityAttributeModel.TABLE, TrackedEntityAttributeModel.Columns.UID, ProgramTrackedEntityAttributeModel.TABLE, ProgramTrackedEntityAttributeModel.Columns.TRACKED_ENTITY_ATTRIBUTE,
            TrackedEntityAttributeModel.TABLE, TrackedEntityAttributeModel.Columns.DISPLAY_IN_LIST_NO_PROGRAM, TrackedEntityAttributeModel.TABLE, TrackedEntityAttributeModel.Columns.UID);
    private Set<String> PROGRAM_TRACKED_ENTITY_ATTRIBUTES_NO_PROGRAM_TABLES = new HashSet<>(Arrays.asList(TrackedEntityAttributeModel.TABLE, ProgramTrackedEntityAttributeModel.TABLE));

    private final String PROGRAM_TRACKED_ENTITY_ATTRIBUTES_VALUES_QUERY = String.format(
            "SELECT %s.* FROM %s " +
                    "JOIN %s ON %s.%s = %s.%s " +
                    "WHERE %s.%s = ? AND %s.%s <> '0' ORDER BY %s.%s ASC",
            TrackedEntityAttributeValueModel.TABLE, TrackedEntityAttributeValueModel.TABLE,
            TrackedEntityAttributeModel.TABLE, TrackedEntityAttributeModel.TABLE, TrackedEntityAttributeModel.Columns.UID, TrackedEntityAttributeValueModel.TABLE, TrackedEntityAttributeValueModel.Columns.TRACKED_ENTITY_ATTRIBUTE,
            TrackedEntityAttributeValueModel.TABLE, TrackedEntityAttributeValueModel.Columns.TRACKED_ENTITY_INSTANCE,
            TrackedEntityAttributeModel.TABLE, TrackedEntityAttributeModel.Columns.SORT_ORDER_IN_LIST_NO_PROGRAM,
            TrackedEntityAttributeModel.TABLE, TrackedEntityAttributeModel.Columns.SORT_ORDER_IN_LIST_NO_PROGRAM
    );
    private final Set<String> ATTR_VALUE_TABLES = new HashSet<>(Arrays.asList(TrackedEntityAttributeValueModel.TABLE, TrackedEntityAttributeModel.TABLE));

    private final String PROGRAM_TRACKED_ENTITY_ATTRIBUTES_VALUES_PROGRAM_QUERY = String.format(
            "SELECT %s.* FROM %s " +
                    "JOIN %s ON %s.%s = %s.%s " +
                    "WHERE %s.%s = ? AND %s.%s = ? ORDER BY %s.%s ASC",
            TrackedEntityAttributeValueModel.TABLE, TrackedEntityAttributeValueModel.TABLE,
            ProgramTrackedEntityAttributeModel.TABLE, ProgramTrackedEntityAttributeModel.TABLE, ProgramTrackedEntityAttributeModel.Columns.TRACKED_ENTITY_ATTRIBUTE, TrackedEntityAttributeValueModel.TABLE, TrackedEntityAttributeValueModel.Columns.TRACKED_ENTITY_ATTRIBUTE,
            ProgramTrackedEntityAttributeModel.TABLE, ProgramTrackedEntityAttributeModel.Columns.PROGRAM,
            TrackedEntityAttributeValueModel.TABLE, TrackedEntityAttributeValueModel.Columns.TRACKED_ENTITY_INSTANCE,
            ProgramTrackedEntityAttributeModel.TABLE, TrackedEntityAttributeModel.Columns.ID);
    private final Set<String> ATTR_PROGRAM_VALUE_TABLES = new HashSet<>(Arrays.asList(TrackedEntityAttributeValueModel.TABLE, ProgramTrackedEntityAttributeModel.TABLE));

    private final String TE_ATTRIBUTE_QUERY = String.format(
            "SELECT * FROM %s WHERE %s.%s = ?",
            TrackedEntityAttributeModel.TABLE, TrackedEntityAttributeModel.TABLE, TrackedEntityAttributeModel.Columns.UID);

    private final String RELATIONSHIP_TYPE_QUERY = String.format("SELECT %s.* FROM %s " +
                    "INNER JOIN %s ON %s.%s = %s.%s  " +
                    "WHERE %s.%s = ",
            RelationshipTypeModel.TABLE, RelationshipTypeModel.TABLE,
            ProgramModel.TABLE, RelationshipTypeModel.TABLE, RelationshipTypeModel.Columns.UID, ProgramModel.TABLE, ProgramModel.Columns.RELATIONSHIP_TYPE,
            ProgramModel.TABLE, ProgramModel.Columns.UID);

    private Set<String> RELATIONSHIP_TYPE_TABLES = new HashSet<>(Arrays.asList(RelationshipTypeModel.TABLE, ProgramModel.TABLE));

    private final String RELATIONSHIP_TYPE_LIST_QUERY = String.format("SELECT * FROM %s ",
            RelationshipTypeModel.TABLE);

    private final String DATA_ELEMENT_QUERY = String.format("SELECT * FROM %s WHERE %s.%s = ",
            DataElementModel.TABLE, DataElementModel.TABLE, DataElementModel.Columns.UID);


    private final String SELECT_PROGRAM_STAGE = String.format("SELECT * FROM %s WHERE %s.%s = ",
            ProgramStageModel.TABLE, ProgramStageModel.TABLE, ProgramStageModel.Columns.UID);

    private final String SELECT_CATEGORY_OPTION = String.format("SELECT * FROM %s WHERE %s.%s = ",
            CategoryOptionModel.TABLE, CategoryOptionModel.TABLE, CategoryOptionModel.Columns.UID);

    private final String SELECT_CATEGORY_OPTION_COMBO = String.format("SELECT * FROM %s WHERE %s.%s = ",
            CategoryOptionComboModel.TABLE, CategoryOptionComboModel.TABLE, CategoryOptionComboModel.Columns.UID);

    private final String SELECT_CATEGORY_COMBO = String.format("SELECT * FROM %s WHERE %s.%s = ",
            CategoryComboModel.TABLE, CategoryComboModel.TABLE, CategoryComboModel.Columns.UID);


    private final BriteDatabase briteDatabase;

    public MetadataRepositoryImpl(@NonNull BriteDatabase briteDatabase) {
        this.briteDatabase = briteDatabase;
    }

    @Override
    public Observable<TrackedEntityModel> getTrackedEntity(String trackedEntityUid) {
        return briteDatabase
                .createQuery(TrackedEntityModel.TABLE, TRACKED_ENTITY_QUERY + "'" + trackedEntityUid + "'")
                .mapToOne(TrackedEntityModel::create);
    }

    @Override
    public Observable<CategoryComboModel> getCategoryComboWithId(String categoryComboId) {
        return briteDatabase
                .createQuery(CategoryComboModel.TABLE, SELECT_CATEGORY_COMBO + "'" + categoryComboId + "'")
                .mapToOne(CategoryComboModel::create);
    }

    public Observable<TrackedEntityInstanceModel> getTrackedEntityInstance(String teiUid) {
        return briteDatabase
                .createQuery(TrackedEntityInstanceModel.TABLE, TRACKED_ENTITY_INSTANCE_QUERY + "'" + teiUid + "'")
                .mapToOne(TrackedEntityInstanceModel::create);
    }

    @Override
    public Observable<CategoryOptionModel> getCategoryOptionWithId(String categoryOptionId) {
        return briteDatabase
                .createQuery(CategoryOptionModel.TABLE, SELECT_CATEGORY_OPTION + "'" + categoryOptionId + "'")
                .mapToOne(CategoryOptionModel::create);
    }

    @Override
    public Observable<CategoryOptionComboModel> getCategoryOptionComboWithId(String categoryOptionComboId) {
        return briteDatabase
                .createQuery(CategoryOptionModel.TABLE, SELECT_CATEGORY_OPTION_COMBO + "'" + categoryOptionComboId + "'")
                .mapToOne(CategoryOptionComboModel::create);
    }

    @Override
    public Observable<OrganisationUnitModel> getOrganisationUnit(String orgUnitUid) {
        return briteDatabase
                .createQuery(OrganisationUnitModel.TABLE, ORG_UNIT_QUERY + "'" + orgUnitUid + "'")
                .mapToOne(OrganisationUnitModel::create);
    }

    @Override
    public Observable<OrganisationUnitModel> getTeiOrgUnit(String teiUid) {
        return briteDatabase
                .createQuery(TEI_ORG_UNIT_TABLES, TEI_ORG_UNIT_QUERY, teiUid)
                .mapToOne(OrganisationUnitModel::create);
    }

    @Override
    public Observable<List<OrganisationUnitModel>> getOrgUnitForOpenAndClosedDate(String currentDate) {
        return briteDatabase
                .createQuery(OrganisationUnitModel.TABLE, ORG_UNIT_DATE_QUERY, currentDate)
                .mapToList(OrganisationUnitModel::create);
    }

    @Override
    public Observable<List<ProgramTrackedEntityAttributeModel>> getProgramTrackedEntityAttributes(String programUid) {
        if (programUid != null)
            return briteDatabase
                    .createQuery(ProgramTrackedEntityAttributeModel.TABLE, PROGRAM_TRACKED_ENTITY_ATTRIBUTES_QUERY + "'" + programUid + "'")
                    .mapToList(ProgramTrackedEntityAttributeModel::create);
        else
            return briteDatabase
                    .createQuery(PROGRAM_TRACKED_ENTITY_ATTRIBUTES_NO_PROGRAM_TABLES, PROGRAM_TRACKED_ENTITY_ATTRIBUTES_NO_PROGRAM_QUERY)
                    .mapToList(ProgramTrackedEntityAttributeModel::create);
    }

    @Override
    public Observable<List<TrackedEntityAttributeValueModel>> getTEIAttributeValues(String uid) {
        return briteDatabase
                .createQuery(ATTR_VALUE_TABLES, PROGRAM_TRACKED_ENTITY_ATTRIBUTES_VALUES_QUERY, uid)
                .mapToList(TrackedEntityAttributeValueModel::create);
    }

    @Override
    public Observable<List<TrackedEntityAttributeValueModel>> getTEIAttributeValues(String programUid, String teiUid) {
        return briteDatabase
                .createQuery(ATTR_PROGRAM_VALUE_TABLES, PROGRAM_TRACKED_ENTITY_ATTRIBUTES_VALUES_PROGRAM_QUERY, programUid, teiUid)
                .mapToList(TrackedEntityAttributeValueModel::create);
    }

    @Override
    public Observable<TrackedEntityAttributeModel> getTrackedEntityAttribute(String teAttribute) {
        return briteDatabase
                .createQuery(TrackedEntityAttributeModel.TABLE, TE_ATTRIBUTE_QUERY, teAttribute)
                .mapToOne(TrackedEntityAttributeModel::create);
    }

    @Override
    public Observable<RelationshipTypeModel> getRelationshipType(String relationshipTypeUid) {
        return briteDatabase
                .createQuery(RELATIONSHIP_TYPE_TABLES, RELATIONSHIP_TYPE_QUERY + "'" + relationshipTypeUid + "'")
                .mapToOneOrDefault(RelationshipTypeModel::create, RelationshipTypeModel.builder().build());

    }

    @Override
    public Observable<List<RelationshipTypeModel>> getRelationshipTypeList() {
        return briteDatabase
                .createQuery(RELATIONSHIP_TYPE_TABLES, RELATIONSHIP_TYPE_LIST_QUERY)
                .mapToList(RelationshipTypeModel::create);
    }

    @NonNull
    @Override
    public Observable<ProgramStageModel> programStage(String programStageId) {
        return briteDatabase
                .createQuery(ProgramStageModel.TABLE, SELECT_PROGRAM_STAGE + "'" + programStageId + "'")
                .mapToOne(ProgramStageModel::create);
    }

    @Override
    public Observable<DataElementModel> getDataElement(String dataElementUid) {
        return briteDatabase
                .createQuery(DataElementModel.TABLE, DATA_ELEMENT_QUERY + "'" + dataElementUid + "'")
                .mapToOne(DataElementModel::create);
    }

    @Override
    public Observable<List<EnrollmentModel>> getTEIEnrollments(String teiUid) {
        return briteDatabase
                .createQuery(EnrollmentModel.TABLE, SELECT_TEI_ENROLLMENTS + "'" + teiUid + "'")
                .mapToList(EnrollmentModel::create);
    }

    @Override
    public Observable<List<ProgramModel>> getTEIProgramsToEnroll(String teiUid) {
        return briteDatabase
                .createQuery(ProgramModel.TABLE, SELECT_PROGRMAS_TO_ENROLL, teiUid)
                .mapToList(ProgramModel::create);
    }

    @Override
    public Observable<EventModel> getEnrollmentLastEvent(String enrollmentUid) {
        return briteDatabase
                .createQuery(SELECT_ENROLLMENT_LAST_EVENT_TABLES, SELECT_ENROLLMENT_LAST_EVENT, enrollmentUid)
                .mapToOne(EventModel::create);
    }

    @Override
    public Observable<Integer> getProgramStageDataElementCount(String programStageId) {
        String SELECT_PROGRAM_STAGE_COUNT = "SELECT COUNT(*) FROM " + ProgramStageDataElementModel.TABLE +
                " WHERE " + ProgramStageDataElementModel.Columns.PROGRAM_STAGE + " = '%s'";
        return briteDatabase
                .createQuery(ProgramStageDataElementModel.TABLE, String.format(SELECT_PROGRAM_STAGE_COUNT, programStageId))
                .mapToOne(cursor -> {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        return cursor.getInt(0);
                    } else
                        return 0;
                });
    }

    @Override
    public Observable<Integer> getTrackEntityDataValueCount(String eventId) {
        String SELECT_TRACKED_ENTITY_COUNT = "SELECT COUNT(*) FROM " + TrackedEntityDataValueModel.TABLE +
                " WHERE " + TrackedEntityDataValueModel.Columns.EVENT + " = '%s'";
        return briteDatabase
                .createQuery(TrackedEntityDataValueModel.TABLE, String.format(SELECT_TRACKED_ENTITY_COUNT, eventId))
                .mapToOne(cursor -> {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        return cursor.getInt(0);
                    } else
                        return 0;
                });
    }

    @Override
    public Observable<List<ProgramModel>> getProgramModelFromEnrollmentList(List<Enrollment> enrollments) {
        String query = "";
        for (Enrollment enrollment : enrollments) {
            query = query.concat(ProgramModel.TABLE + "." + ProgramModel.Columns.UID + " = '" + enrollment.program() + "'");
            if (!enrollment.program().equals(enrollments.get(enrollments.size() - 1).program()))
                query = query.concat(" OR ");
        }

        return briteDatabase
                .createQuery(ProgramModel.TABLE, PROGRAM_LIST_QUERY + query)
                .mapToList(ProgramModel::create);

    }

    @Override
    public Observable<List<ProgramModel>> getTeiActivePrograms(String teiUid) {
        return briteDatabase.createQuery(ACTIVE_TEI_PROGRAMS_TABLES, ACTIVE_TEI_PROGRAMS, teiUid)
                .mapToList(ProgramModel::create);
    }

    @Override
    public Observable<ProgramModel> getProgramWithId(String programUid) {
        return briteDatabase
                .createQuery(ProgramModel.TABLE, PROGRAM_LIST_ALL_QUERY + "'" + programUid + "'")
                .mapToOne(ProgramModel::create);
    }
}
