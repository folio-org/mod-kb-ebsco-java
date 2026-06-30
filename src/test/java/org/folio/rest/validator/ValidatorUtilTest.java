package org.folio.rest.validator;

import static org.folio.rest.validator.ValidatorUtil.checkDateValid;
import static org.folio.rest.validator.ValidatorUtil.checkDatesOrder;
import static org.folio.rest.validator.ValidatorUtil.checkFalseOrNull;
import static org.folio.rest.validator.ValidatorUtil.checkInRange;
import static org.folio.rest.validator.ValidatorUtil.checkIsBlank;
import static org.folio.rest.validator.ValidatorUtil.checkIsEmpty;
import static org.folio.rest.validator.ValidatorUtil.checkIsEmptyCollection;
import static org.folio.rest.validator.ValidatorUtil.checkIsEqual;
import static org.folio.rest.validator.ValidatorUtil.checkIsNotAllBlank;
import static org.folio.rest.validator.ValidatorUtil.checkIsNotBlank;
import static org.folio.rest.validator.ValidatorUtil.checkIsNotEmpty;
import static org.folio.rest.validator.ValidatorUtil.checkIsNotNull;
import static org.folio.rest.validator.ValidatorUtil.checkIsNull;
import static org.folio.rest.validator.ValidatorUtil.checkMaxLength;
import static org.folio.rest.validator.ValidatorUtil.checkNoHtml;
import static org.folio.rest.validator.ValidatorUtil.checkUrlFormat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import org.folio.rest.exception.InputValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ValidatorUtilTest {

  @Nested
  class CheckIsNotEmpty {

    @Test
    void shouldThrow_whenValueIsEmpty() {
      var ex = assertThrows(InputValidationException.class, () -> checkIsNotEmpty("field", ""));
      assertEquals("Invalid field", ex.getMessage());
      assertEquals("field must not be empty", ex.getMessageDetail());
    }

    @Test
    void shouldThrow_whenValueIsNull() {
      var ex = assertThrows(InputValidationException.class, () -> checkIsNotEmpty("field", null));
      assertEquals("Invalid field", ex.getMessage());
      assertEquals("field must not be empty", ex.getMessageDetail());
    }

    @Test
    void shouldNotThrow_whenValueIsPresent() {
      assertDoesNotThrow(() -> checkIsNotEmpty("field", "value"));
    }
  }

  @Nested
  class CheckIsEmpty {

    @Test
    void shouldThrow_whenValueIsNotEmpty() {
      var ex = assertThrows(InputValidationException.class, () -> checkIsEmpty("field", "value"));
      assertEquals("Invalid field", ex.getMessage());
      assertEquals("field must be empty", ex.getMessageDetail());
    }

    @Test
    void shouldNotThrow_whenValueIsEmpty() {
      assertDoesNotThrow(() -> checkIsEmpty("field", ""));
    }

    @Test
    void shouldNotThrow_whenValueIsNull() {
      assertDoesNotThrow(() -> checkIsEmpty("field", null));
    }
  }

  @Nested
  class CheckIsBlank {

    @Test
    void shouldThrow_whenValueIsNotBlank() {
      var ex = assertThrows(InputValidationException.class, () -> checkIsBlank("field", "text"));
      assertEquals("Invalid field", ex.getMessage());
      assertEquals("field must be empty", ex.getMessageDetail());
    }

    @Test
    void shouldNotThrow_whenValueIsBlank() {
      assertDoesNotThrow(() -> checkIsBlank("field", "   "));
    }

    @Test
    void shouldNotThrow_whenValueIsNull() {
      assertDoesNotThrow(() -> checkIsBlank("field", null));
    }
  }

  @Nested
  class CheckIsNotBlank {

    @Test
    void shouldThrow_whenValueIsBlank() {
      var ex = assertThrows(InputValidationException.class, () -> checkIsNotBlank("field", "   "));
      assertEquals("Invalid field", ex.getMessage());
      assertEquals("field must not be empty", ex.getMessageDetail());
    }

    @Test
    void shouldThrow_whenValueIsNull() {
      var ex = assertThrows(InputValidationException.class, () -> checkIsNotBlank("field", null));
      assertEquals("Invalid field", ex.getMessage());
      assertEquals("field must not be empty", ex.getMessageDetail());
    }

    @Test
    void shouldNotThrow_whenValueHasContent() {
      assertDoesNotThrow(() -> checkIsNotBlank("field", "text"));
    }
  }

  @Nested
  class CheckIsNotAllBlank {

    @Test
    void shouldThrow_whenAllValuesAreBlank() {
      var ex = assertThrows(InputValidationException.class,
        () -> checkIsNotAllBlank("field", "", "  ", null));
      assertEquals("Invalid field", ex.getMessage());
      assertEquals("At least one of field must not be empty", ex.getMessageDetail());
    }

    @Test
    void shouldNotThrow_whenAtLeastOneValueIsNotBlank() {
      assertDoesNotThrow(() -> checkIsNotAllBlank("field", "", "value"));
    }
  }

  @Nested
  class CheckFalseOrNull {

    @Test
    void shouldThrow_whenValueIsTrue() {
      var ex = assertThrows(InputValidationException.class, () -> checkFalseOrNull("field", true));
      assertEquals("Invalid field", ex.getMessage());
      assertEquals("field must be false", ex.getMessageDetail());
    }

    @Test
    void shouldNotThrow_whenValueIsFalse() {
      assertDoesNotThrow(() -> checkFalseOrNull("field", false));
    }

    @Test
    void shouldNotThrow_whenValueIsNull() {
      assertDoesNotThrow(() -> checkFalseOrNull("field", null));
    }
  }

  @Nested
  class CheckIsNotNull {

    @Test
    void shouldThrow_whenValueIsNull() {
      var ex = assertThrows(InputValidationException.class, () -> checkIsNotNull("field", null));
      assertEquals("Invalid field", ex.getMessage());
      assertEquals("field must not be null", ex.getMessageDetail());
    }

    @Test
    void shouldNotThrow_whenValueIsPresent() {
      assertDoesNotThrow(() -> checkIsNotNull("field", "value"));
    }
  }

  @Nested
  class CheckIsNull {

    @Test
    void shouldThrow_whenValueIsNotNull() {
      var ex = assertThrows(InputValidationException.class, () -> checkIsNull("field", "value"));
      assertEquals("Invalid field", ex.getMessage());
      assertEquals("field must be null or not specified", ex.getMessageDetail());
    }

    @Test
    void shouldNotThrow_whenValueIsNull() {
      assertDoesNotThrow(() -> checkIsNull("field", null));
    }
  }

  @Nested
  class CheckIsEmptyCollection {

    @Test
    void shouldThrow_whenCollectionIsNotEmpty() {
      var collection = List.of("item");
      var ex = assertThrows(InputValidationException.class, () -> checkIsEmptyCollection("field", collection));
      assertEquals("Invalid field", ex.getMessage());
      assertEquals("field must be null or not specified", ex.getMessageDetail());
    }

    @Test
    void shouldNotThrow_whenCollectionIsEmpty() {
      var collection = Collections.emptyList();
      assertDoesNotThrow(() -> checkIsEmptyCollection("field", collection));
    }

    @Test
    void shouldNotThrow_whenCollectionIsNull() {
      assertDoesNotThrow(() -> checkIsEmptyCollection("field", null));
    }
  }

  @Nested
  class CheckDateValid {

    @Test
    void shouldThrow_whenDateFormatIsInvalid() {
      var ex = assertThrows(InputValidationException.class, () -> checkDateValid("date", "2024/01/01"));
      assertEquals("Invalid date", ex.getMessage());
      assertEquals("date has invalid format. Should be YYYY-MM-DD", ex.getMessageDetail());
    }

    @Test
    void shouldThrow_whenDateHasInvalidValue() {
      var ex = assertThrows(InputValidationException.class, () -> checkDateValid("date", "not-a-date"));
      assertEquals("Invalid date", ex.getMessage());
      assertEquals("date has invalid format. Should be YYYY-MM-DD", ex.getMessageDetail());
    }

    @Test
    void shouldNotThrow_whenDateIsValid() {
      assertDoesNotThrow(() -> checkDateValid("date", "2024-01-15"));
    }

    @Test
    void shouldNotThrow_whenDateIsEmpty() {
      assertDoesNotThrow(() -> checkDateValid("date", ""));
    }

    @Test
    void shouldNotThrow_whenDateIsNull() {
      assertDoesNotThrow(() -> checkDateValid("date", null));
    }
  }

  @Nested
  class CheckMaxLength {

    @Test
    void shouldThrow_whenValueExceedsMaxLength() {
      var ex = assertThrows(InputValidationException.class,
        () -> checkMaxLength("field", "toolongvalue", 5));
      assertEquals("Invalid field", ex.getMessage());
      assertEquals("field is too long (maximum is 5 characters)", ex.getMessageDetail());
    }

    @Test
    void shouldNotThrow_whenValueEqualsMaxLength() {
      assertDoesNotThrow(() -> checkMaxLength("field", "exact", 5));
    }

    @Test
    void shouldNotThrow_whenValueIsShorterThanMaxLength() {
      assertDoesNotThrow(() -> checkMaxLength("field", "ok", 5));
    }

    @Test
    void shouldNotThrow_whenValueIsNull() {
      assertDoesNotThrow(() -> checkMaxLength("field", null, 5));
    }
  }

  @Nested
  class CheckDatesOrder {

    @Test
    void shouldThrow_whenStartIsAfterEnd() {
      var ex = assertThrows(InputValidationException.class,
        () -> checkDatesOrder("2024-06-01", "2024-01-01"));
      assertEquals("Begin Coverage should be smaller than End Coverage", ex.getMessage());
      assertEquals("", ex.getMessageDetail());
    }

    @Test
    void shouldNotThrow_whenStartIsBeforeEnd() {
      assertDoesNotThrow(() -> checkDatesOrder("2024-01-01", "2024-06-01"));
    }

    @Test
    void shouldNotThrow_whenStartEqualsEnd() {
      assertDoesNotThrow(() -> checkDatesOrder("2024-01-01", "2024-01-01"));
    }
  }

  @Nested
  class CheckUrlFormat {

    @Test
    void shouldThrow_whenUrlIsInvalid() {
      var ex = assertThrows(InputValidationException.class,
        () -> checkUrlFormat("url", "not a url"));
      assertEquals("Invalid url", ex.getMessage());
      assertEquals("url has invalid format. Should start with https:// or http://", ex.getMessageDetail());
    }

    @Test
    void shouldNotThrow_whenUrlIsHttps() {
      assertDoesNotThrow(() -> checkUrlFormat("url", "https://example.com"));
    }

    @Test
    void shouldNotThrow_whenUrlIsHttp() {
      assertDoesNotThrow(() -> checkUrlFormat("url", "http://example.com"));
    }
  }

  @Nested
  class CheckInRange {

    @Test
    void shouldThrow_whenValueIsNull() {
      var ex = assertThrows(InputValidationException.class,
        () -> checkInRange("field", null, 1, 10));
      assertEquals("Invalid field", ex.getMessage());
      assertEquals("field should be in range 1 - 10", ex.getMessageDetail());
    }

    @Test
    void shouldThrow_whenValueIsBelowMin() {
      var ex = assertThrows(InputValidationException.class, () -> checkInRange("field", 0, 1, 10));
      assertEquals("Invalid field", ex.getMessage());
      assertEquals("field should be in range 1 - 10", ex.getMessageDetail());
    }

    @Test
    void shouldThrow_whenValueIsAboveMax() {
      var ex = assertThrows(InputValidationException.class, () -> checkInRange("field", 11, 1, 10));
      assertEquals("Invalid field", ex.getMessage());
      assertEquals("field should be in range 1 - 10", ex.getMessageDetail());
    }

    @Test
    void shouldNotThrow_whenValueIsAtMin() {
      assertDoesNotThrow(() -> checkInRange("field", 1, 1, 10));
    }

    @Test
    void shouldNotThrow_whenValueIsAtMax() {
      assertDoesNotThrow(() -> checkInRange("field", 10, 1, 10));
    }

    @Test
    void shouldNotThrow_whenValueIsInRange() {
      assertDoesNotThrow(() -> checkInRange("field", 5, 1, 10));
    }
  }

  @Nested
  class CheckNoHtml {

    @Test
    void shouldThrow_whenValueContainsHtmlTag() {
      var ex = assertThrows(InputValidationException.class,
        () -> checkNoHtml("field", "<b>bold</b>", "HTML not allowed"));
      assertEquals("Invalid field", ex.getMessage());
      assertEquals("HTML not allowed", ex.getMessageDetail());
    }

    @Test
    void shouldThrow_whenValueContainsSelfClosingTag() {
      var ex = assertThrows(InputValidationException.class,
        () -> checkNoHtml("field", "text <br/> more", "HTML not allowed"));
      assertEquals("Invalid field", ex.getMessage());
      assertEquals("HTML not allowed", ex.getMessageDetail());
    }

    @Test
    void shouldNotThrow_whenValueHasNoHtml() {
      assertDoesNotThrow(() -> checkNoHtml("field", "plain text", "HTML not allowed"));
    }

    @Test
    void shouldNotThrow_whenValueIsNull() {
      assertDoesNotThrow(() -> checkNoHtml("field", null, "HTML not allowed"));
    }
  }

  @Nested
  class CheckIsEqual {

    @Test
    void shouldThrow_whenValuesAreNotEqual() {
      var ex = assertThrows(InputValidationException.class,
        () -> checkIsEqual("field", "expected", "actual"));
      assertEquals("Invalid field", ex.getMessage());
      assertEquals("field should be equals to 'expected' but actual is 'actual'", ex.getMessageDetail());
    }

    @Test
    void shouldNotThrow_whenValuesAreEqual() {
      assertDoesNotThrow(() -> checkIsEqual("field", "value", "value"));
    }
  }
}
