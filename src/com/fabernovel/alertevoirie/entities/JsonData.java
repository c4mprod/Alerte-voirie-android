/**
 * This file is part of the Alerte Voirie project.
 * 
 * Copyright (C) 2010-2011 C4M PROD
 * 
 * Alerte Voirie is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alerte Voirie is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Alerte Voirie.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.fabernovel.alertevoirie.entities;

public interface JsonData {

    String VALUE_REQUEST_GET_INCIDENTS_BY_POSITION = "getIncidentsByPosition";
    String VALUE_REQUEST_GET_USERS_ACTVITIES       = "getUsersActivities";
    String VALUE_REQUEST_GET_INCIDENTS_STATS       = "getIncidentStats";
    String VALUE_REQUEST_NEW_INCIDENT              = "saveIncident";
    String VALUE_REQUEST_GET_MY_INCIDENTS          = "getReports";
    String VALUE_REQUEST_GET_IMAGES                = "getIncidentPhotos";

    String VALUE_RADIUS_FAR                        = "far";
    String VALUE_RADIUS_CLOSE                      = "close";
    int    VALUE_INCIDENT_SAVED                    = 0;
    String VALUE_NULL                              = "null";

    String PARAM_REQUEST                           = "request";
    String PARAM_ANSWER                            = "answer";

    String PARAM_UDID                              = "udid";
    String PARAM_RADIUS                            = "radius";
    String PARAM_STATUS                            = "status";
    String PARAM_CLOSEST_INCIDENTS                 = "closest_incidents";
    String PARAM_UPDATED_INCIDENTS                 = "updated_incidents";
    String PARAM_ONGOING_INCIDENTS                 = "ongoing_incidents";
    String PARAM_RESOLVED_INCIDENTS                = "resolved_incidents";
    String PARAM_CATEGORY_CHILDREN                 = "children_id";

    String PARAM_POSITION                          = "position";
    String PARAM_POSITION_LONGITUDE                = "longitude";
    String PARAM_POSITION_LATITUDE                 = "latitude";

    String PARAM_INCIDENT                          = "incident";
    String PARAM_INCIDENT_OBJECT                   = "incidentObj";
    String PARAM_INCIDENT_DESCRIPTION              = "descriptive";
    String PARAM_INCIDENT_ADDRESS                  = "address";
    String PARAM_INCIDENT_DATE                     = "date";
    String PARAM_INCIDENT_STATUS                   = "state";
    String PARAM_INCIDENT_CATEGORY                 = "categoryId";
    String PARAM_INCIDENT_PICTURES                 = "pictures";
    String PARAM_INCIDENT_PICTURES_CLOSE           = "close";
    String PARAM_INCIDENT_PICTURES_FAR             = "far";
    String PARAM_INCIDENT_LATITUDE                 = "lat";
    String PARAM_INCIDENT_LONGITUDE                = "lng";
    String PARAM_INCIDENT_ID                       = "id";
    String PARAM_INCIDENT_CONFIRMS                 = "confirms";
    String PARAM_INCIDENTS                         = "incidents";

    String ANSWER_INCIDENT_ID                      = "incidentId";
    String PARAM_DECLARED_INCIDENTS                = "declared_incidents";

    String VALUE_REQUEST_UPDATE_INCIDENT           = "updateIncident";
    String PARAM_UPDATE_INCIDENT_RESOLVED          = "Resolved";
    String PARAM_UPDATE_INCIDENT_CONFIRMED         = "Confirmed";
    String PARAM_UPDATE_INCIDENT_INVALID           = "Invalid";
    String PARAM_UPDATE_INCIDENT_NEW               = "NewIncident";
    String PARAM_UPDATE_INCIDENT_PHOTO             = "Photo";
    String PARAM_UPDATE_INCIDENT_LOG               = "incidentLog";
    String ANSWER_CLOSEST_INCIDENTS                = "losest_incidents";
    String PARAM_INCIDENT_INVALIDATION             = "invalidations";
    String PARAM_INCIDENT_LOG                      = "incidentLog";

    String PARAM_IMAGES_INCIDENT_ID                = "incidentId";
    String PARAM_PHOTOS                            = "photos";
    String PARAM_IMAGES_DATE                       = "date";
    String PARAM_IMAGES_COMMENT                    = "commentary";
    String PARAM_IMAGES_URL                        = "photo_url";
    String VALUE_REQUEST_CHANGE_INCIDENT = "changeIncident";
}
