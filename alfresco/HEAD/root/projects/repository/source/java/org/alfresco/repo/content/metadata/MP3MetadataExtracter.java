/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.content.metadata;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.farng.mp3.AbstractMP3FragmentBody;
import org.farng.mp3.MP3File;
import org.farng.mp3.TagException;
import org.farng.mp3.id3.AbstractID3v2;
import org.farng.mp3.id3.AbstractID3v2Frame;
import org.farng.mp3.id3.ID3v1;
import org.farng.mp3.lyrics3.AbstractLyrics3;
import org.farng.mp3.lyrics3.Lyrics3v2;
import org.farng.mp3.lyrics3.Lyrics3v2Field;

/**
 * @author Roy Wetherall
 */
public class MP3MetadataExtracter extends AbstractMetadataExtracter
{
    private static final QName PROP_ALBUM_TITLE = QName.createQName("{music}albumTitle");
    private static final QName PROP_SONG_TITLE = QName.createQName("{music}songTitle");;
    private static final QName PROP_ARTIST = QName.createQName("{music}artist");;
    private static final QName PROP_COMMENT = QName.createQName("{music}comment");;
    private static final QName PROP_YEAR_RELEASED = QName.createQName("{music}yearReleased");;
    private static final QName PROP_TRACK_NUMBER = QName.createQName("{music}trackNumber");;
    private static final QName PROP_GENRE = QName.createQName("{music}genre");;
    private static final QName PROP_COMPOSER = QName.createQName("{music}composer");;
    private static final QName PROP_LYRICS = QName.createQName("{music}lyrics");;

    public MP3MetadataExtracter()
    {
        super(MimetypeMap.MIMETYPE_MP3, 1.0, 1000);
    }

    /**
     * @see org.alfresco.repo.content.metadata.MetadataExtracter#extract(org.alfresco.service.cmr.repository.ContentReader, java.util.Map)
     */
    public void extract(ContentReader reader,
            Map<QName, Serializable> destination) throws ContentIOException
    {
        try
        {
            Map<QName, Serializable> props = new HashMap<QName, Serializable>();            
            
            // Create a temp file
            File tempFile = File.createTempFile(GUID.generate(), ".tmp");
            try
            {
                reader.getContent(tempFile);
                
                // Create the MP3 object from the file
                MP3File mp3File = new MP3File(tempFile);
                
                ID3v1 id3v1 = mp3File.getID3v1Tag();
                if (id3v1 != null)
                {
                    setTagValue(props, PROP_ALBUM_TITLE, id3v1.getAlbum());
                    setTagValue(props, PROP_SONG_TITLE, id3v1.getTitle());
                    setTagValue(props, PROP_ARTIST, id3v1.getArtist());
                    setTagValue(props, PROP_COMMENT, id3v1.getComment());
                    setTagValue(props, PROP_YEAR_RELEASED, id3v1.getYear());
                    
                    // TODO sort out the genre
                    //setTagValue(props, MusicModel.PROP_GENRE, id3v1.getGenre());
                    
                    // TODO sort out the size
                    //setTagValue(props, MusicModel.PROP_SIZE, id3v1.getSize());            
                }
                
                AbstractID3v2 id3v2 = mp3File.getID3v2Tag();
                if (id3v2 != null)
                {
                    setTagValue(props, PROP_SONG_TITLE, getID3V2Value(id3v2, "TIT2"));
                    setTagValue(props, PROP_ARTIST, getID3V2Value(id3v2, "TPE1"));
                    setTagValue(props, PROP_ALBUM_TITLE, getID3V2Value(id3v2, "TALB"));
                    setTagValue(props, PROP_YEAR_RELEASED, getID3V2Value(id3v2, "TDRC"));
                    setTagValue(props, PROP_COMMENT, getID3V2Value(id3v2, "COMM"));
                    setTagValue(props, PROP_TRACK_NUMBER, getID3V2Value(id3v2, "TRCK"));
                    setTagValue(props, PROP_GENRE, getID3V2Value(id3v2, "TCON"));
                    setTagValue(props, PROP_COMPOSER, getID3V2Value(id3v2, "TCOM"));
                    
                    // TODO sort out the lyrics
                    //System.out.println("Lyrics: " + getID3V2Value(id3v2, "SYLT"));
                    //System.out.println("Lyrics: " + getID3V2Value(id3v2, "USLT"));
                }
                
                AbstractLyrics3 lyrics3Tag = mp3File.getLyrics3Tag();
                if (lyrics3Tag != null)
                {
                    System.out.println("Lyrics3 tag found.");
                    if (lyrics3Tag instanceof Lyrics3v2)
                    {
                        setTagValue(props, PROP_SONG_TITLE, getLyrics3v2Value((Lyrics3v2)lyrics3Tag, "TIT2"));
                        setTagValue(props, PROP_ARTIST, getLyrics3v2Value((Lyrics3v2)lyrics3Tag, "TPE1"));
                        setTagValue(props, PROP_ALBUM_TITLE, getLyrics3v2Value((Lyrics3v2)lyrics3Tag, "TALB"));
                        setTagValue(props, PROP_COMMENT, getLyrics3v2Value((Lyrics3v2)lyrics3Tag, "COMM"));
                        setTagValue(props, PROP_LYRICS, getLyrics3v2Value((Lyrics3v2)lyrics3Tag, "SYLT"));
                        setTagValue(props, PROP_COMPOSER, getLyrics3v2Value((Lyrics3v2)lyrics3Tag, "TCOM"));
                    }
                }
                
            }
            finally
            {
                tempFile.delete();
            }
            
            // Set the destination values
            if (props.get(PROP_SONG_TITLE) != null)
            {
                destination.put(ContentModel.PROP_TITLE, props.get(PROP_SONG_TITLE));
            }
            if (props.get(PROP_ARTIST) != null)
            {
                destination.put(ContentModel.PROP_CREATOR, props.get(PROP_ARTIST));
            }
            String description = getDescription(props);
            if (description != null)
            {
                destination.put(ContentModel.PROP_DESCRIPTION, description);
            }
        }       
        catch (IOException ioException)
        {
            // TODO sort out exception handling
            throw new RuntimeException("Error reading mp3 file.", ioException);
        }
        catch (TagException tagException)
        {
            // TODO sort out exception handling
            throw new RuntimeException("Error reading mp3 tag information.", tagException);
        }
    }
    

    /**
     * Generate the description
     * 
     * @param props     the properties extracted from the file
     * @return          the description
     */
    private String getDescription(Map<QName, Serializable> props)
    {
        StringBuilder result = new StringBuilder();
        if (props.get(PROP_SONG_TITLE) != null && props.get(PROP_ARTIST) != null && props.get(PROP_ALBUM_TITLE) != null)
        {
            result
                .append(props.get(PROP_SONG_TITLE))
                .append(" - ")
                .append(props.get(PROP_ALBUM_TITLE))
                .append(" (")
                .append(props.get(PROP_ARTIST))
                .append(")");
                
        }
        
        return result.toString();
    }

    /**
     * 
     * @param props
     * @param propQName
     * @param propvalue
     */
    private void setTagValue(Map<QName, Serializable> props, QName propQName, String propvalue)
    {
        if (propvalue != null && propvalue.length() != 0)
        {
            trimPut(propQName, propvalue, props);
        }       
    }

    /**
     * 
     * @param lyrics3Tag
     * @param name
     * @return
     */
    private String getLyrics3v2Value(Lyrics3v2 lyrics3Tag, String name) 
    {
        String result = "";
        Lyrics3v2Field field = lyrics3Tag.getField(name);
        if (field != null)
        {
            AbstractMP3FragmentBody body = field.getBody();
            if (body != null)
            {
                result = (String)body.getObject("Text");                
            }
        }
        return result;
    }

    /**
     * Get the ID3V2 tag value in a safe way
     * 
     * @param id3v2
     * @param name
     * @return
     */
    private String getID3V2Value(AbstractID3v2 id3v2, String name)
    {
        String result = "";
        
        AbstractID3v2Frame frame = id3v2.getFrame(name);
        if (frame != null)
        {
            AbstractMP3FragmentBody body = frame.getBody();
            if (body != null)
            {
                result = (String)body.getObject("Text");                
            }
        }
        
        return result;
    }

}
