import $file.circe

/*
read in a  swagger file

generate fs2-http code


Common Name	type	format	Comments
integer	integer	int32	signed 32 bits
long	integer	int64	signed 64 bits
float	number	float
double	number	double
string	string
byte	string	byte	base64 encoded characters
binary	string	binary	any sequence of octets
boolean	boolean
date	string	date	As defined by full-date - RFC3339
dateTime	string	date-time	As defined by date-time - RFC3339
password	string	password	Used to hint UIs the input needs to be obscured.

*/
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._

sealed trait SwaggerType

object IntegerType extends Enumeration {
  type SwaggerType = Value

  val integer = Value("integer")
  val number = Value("number")
  val string = Value("string")
  val boolean = Value("boolean")
}

object SwaggerNumber extends Enumeration {
  type SwaggerNumber = Value

  val float = Value("float")
  val double = Value("double")
}

object SwaggerInteger extends Enumeration {
  type SwaggerInteger = Value

  val int32 = Value("int32")
  val int64 = Value("int64")
}

type SwaggerDataType = IntegerType :+: SwaggerNumber :+: SwaggerInteger :+: CNil
