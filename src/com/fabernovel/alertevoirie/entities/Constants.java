/**
 * This file is part of the Alerte Voirie project.
 * 
 * Copyright (C) 2010-2011 C4M PROD
 * 
 * Alerte Voirie is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alerte Voirie is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alerte Voirie.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.fabernovel.alertevoirie.entities;

public interface Constants {
    public static final String  PROJECT_TAG            = "Alerte Voirie";
    public static final String  RESOURCES_PACKAGE      = "com.fabernovel.alertevoirie";
    public static final int     PICTURE_PREFERED_WIDTH = 640;
    public static final String  NEW_REPORT             = "NewReport";
    public static final String  SDCARD_PATH            = "/Android/data/" + RESOURCES_PACKAGE;
    public static final boolean DEBUGMODE              = false;
    public static final long    TIMEOUT                = 20000;
}
