package views.html.helper

/** Generates personalized helper to represent data into the current design (materialize).
 *  used to the courtesy of CrossStream.ch */
object RichHelpers {
  implicit val materializeForms = FieldConstructor(f => views.html.helpers.formfield(f))
}