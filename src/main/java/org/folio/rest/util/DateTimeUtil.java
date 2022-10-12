package org.folio.rest.util;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class to work with DateTime.
 */
@UtilityClass
public class DateTimeUtil {

  public static final DateTimeFormatter POSTGRES_TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
  public static final DateTimeFormatter POSTGRES_TIMESTAMP_OLD_FORMATTER = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .append(DateTimeFormatter.ISO_LOCAL_DATE)
      .appendLiteral(' ')
      .append(DateTimeFormatter.ISO_LOCAL_TIME)
      .appendOffset("+HH", "Z")
      .toFormatter();

  private static final Logger logger = LogManager.getLogger(DateTimeUtil.class);
  private static final String PARSE_EXCEPTION_MESSAGE = "Error parsing string: {}. Trying to parse the old format";

  /**
   * Retrieves current date time by default time-zone.
   *
   * @return {@link String} of current date time
   */
  public static String getTimeNow() {
    return POSTGRES_TIMESTAMP_FORMATTER.format(OffsetDateTime.now());
  }

  /**
   * Parse string to zoned date time.
   * Supporting old format.
   *
   * @param stringToParse - date time string
   * @return {@link OffsetDateTime} parsed from string
   */
  public static OffsetDateTime getZonedDateTime(String stringToParse) {
    try {
      return OffsetDateTime.parse(stringToParse, POSTGRES_TIMESTAMP_FORMATTER);
    } catch (DateTimeParseException parseException) {
      logger.warn(PARSE_EXCEPTION_MESSAGE, stringToParse);
      return OffsetDateTime.parse(stringToParse, POSTGRES_TIMESTAMP_OLD_FORMATTER);
    }
  }
}
