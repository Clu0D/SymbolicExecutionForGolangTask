package main

import (
)

func test_CompareTwoDifferentObjectsFromArguments() {

	// Test case 1
	804@param#snd := 1
	803@param#fst := 1
	797@param#o := 1
    result := CompareTwoDifferentObjectsFromArguments(804@param#snd, 803@param#fst, 797@param#o)
	if result != 2 {
	    t.Errorf("test_CompareTwoDifferentObjectsFromArguments1 failed")
	}
	
	// Test case 2
	804@param#snd := 1
	803@param#fst := 2
	797@param#o := 2
    result := CompareTwoDifferentObjectsFromArguments(804@param#snd, 803@param#fst, 797@param#o)
	if result != 3 {
	    t.Errorf("test_CompareTwoDifferentObjectsFromArguments2 failed")
	}
	
	// Test case 3
	804@param#snd := 1
	803@param#fst := 1
	797@param#o := 1
    result := CompareTwoDifferentObjectsFromArguments(804@param#snd, 803@param#fst, 797@param#o)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_CompareTwoDifferentObjectsFromArguments3 failed")
	}
	
}

func test_DefaultValue() {

	// Test case 1
	805@param#o := 1
    result := DefaultValue(805@param#o)
	if result != G*NAMED:ObjectWithRefFieldClass {
	    t.Errorf("test_DefaultValue1 failed")
	}
	
}

func test_Equals() {

	// Test case 1
	819@param#other := 2
	818@param#o := 1
    result := Equals(819@param#other, 818@param#o)
	if result != true {
	    t.Errorf("test_Equals1 failed")
	}
	
}

func test_CompareTwoIdenticalObjectsFromArguments() {

	// Test case 1
	827@param#snd := 1
	826@param#fst := 2
	820@param#o := 2
    result := CompareTwoIdenticalObjectsFromArguments(827@param#snd, 826@param#fst, 820@param#o)
	if result != 2 {
	    t.Errorf("test_CompareTwoIdenticalObjectsFromArguments1 failed")
	}
	
	// Test case 2
	827@param#snd := 2
	826@param#fst := 2
	820@param#o := 2
    result := CompareTwoIdenticalObjectsFromArguments(827@param#snd, 826@param#fst, 820@param#o)
	if result != 1 {
	    t.Errorf("test_CompareTwoIdenticalObjectsFromArguments2 failed")
	}
	
}

func test_WriteObjectField() {

	// Test case 1
	828@param#r := 1
	830@param#node := 1
    result := WriteObjectField(828@param#r, 830@param#node)
	if result != G*NAMED:RecursiveTypeClass {
	    t.Errorf("test_WriteObjectField1 failed")
	}
	
}

func test_CompareTwoRefEqualObjects() {

	// Test case 1
	835@param#a := 0
	834@param#o := 2
    result := CompareTwoRefEqualObjects(835@param#a, 834@param#o)
	if result != 1 {
	    t.Errorf("test_CompareTwoRefEqualObjects1 failed")
	}
	
}

func test_CompareTwoObjectsWithNullRefField() {

	// Test case 1
	848@param#snd := 1
	847@param#fst := 1
	841@param#o := 2
    result := CompareTwoObjectsWithNullRefField(848@param#snd, 847@param#fst, 841@param#o)
	if result != 1 {
	    t.Errorf("test_CompareTwoObjectsWithNullRefField1 failed")
	}
	
	// Test case 2
	848@param#snd := 2
	847@param#fst := 1
	841@param#o := 1
    result := CompareTwoObjectsWithNullRefField(848@param#snd, 847@param#fst, 841@param#o)
	if result != 2 {
	    t.Errorf("test_CompareTwoObjectsWithNullRefField2 failed")
	}
	
}

func test_FieldWithDefaultValue() {

	// Test case 1
	855@param#y := 2
	854@param#x := 0
	853@param#o := 1
    result := FieldWithDefaultValue(855@param#y, 854@param#x, 853@param#o)
	if result != G*NAMED:ObjectWithPrimitivesClass {
	    t.Errorf("test_FieldWithDefaultValue1 failed")
	}
	
}

func test_Memory() {

	// Test case 1
	868@param#value := 318
	867@param#objectExample := 1
	861@param#o := 1
    result := Memory(868@param#value, 867@param#objectExample, 861@param#o)
	if result != G*NAMED:ObjectWithPrimitivesClass {
	    t.Errorf("test_Memory1 failed")
	}
	
	// Test case 2
	868@param#value := -5
	867@param#objectExample := 1
	861@param#o := 2
    result := Memory(868@param#value, 867@param#objectExample, 861@param#o)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_Memory2 failed")
	}
	
}

func test_CompareTwoObjectsWithTheSameRefField() {

	// Test case 1
	876@param#snd := 3
	869@param#o := 2
	875@param#fst := 1
    result := CompareTwoObjectsWithTheSameRefField(876@param#snd, 869@param#o, 875@param#fst)
	if result != 2 {
	    t.Errorf("test_CompareTwoObjectsWithTheSameRefField1 failed")
	}
	
	// Test case 2
	876@param#snd := 2
	869@param#o := 2
	875@param#fst := 2
    result := CompareTwoObjectsWithTheSameRefField(876@param#snd, 869@param#o, 875@param#fst)
	if result != 1 {
	    t.Errorf("test_CompareTwoObjectsWithTheSameRefField2 failed")
	}
	
}

func test_CompareTwoDifferentObjects() {

	// Test case 1
	882@param#a := 301
	881@param#o := 1
    result := CompareTwoDifferentObjects(882@param#a, 881@param#o)
	if result != 1 {
	    t.Errorf("test_CompareTwoDifferentObjects1 failed")
	}
	
}

func test_CompareTwoNullObjects() {

	// Test case 1
	888@param#o := 2
	889@param#value := -15
    result := CompareTwoNullObjects(888@param#o, 889@param#value)
	if result != 1 {
	    t.Errorf("test_CompareTwoNullObjects1 failed")
	}
	
}

func test_InheritorsFields() {

	// Test case 1
	902@param#fst := 1
	903@param#snd := 1
	895@param#o := 1
    result := InheritorsFields(902@param#fst, 903@param#snd, 895@param#o)
	if result != 1 {
	    t.Errorf("test_InheritorsFields1 failed")
	}
	
}

func test_ReadFromArrayField() {

	// Test case 1
	904@param#o := 1
	911@param#value := 0
	910@param#objectExample := 1
    result := ReadFromArrayField(904@param#o, 911@param#value, 910@param#objectExample)
	if result != 1 {
	    t.Errorf("test_ReadFromArrayField1 failed")
	}
	
	// Test case 2
	904@param#o := 1
	911@param#value := 1
	910@param#objectExample := 2
    result := ReadFromArrayField(904@param#o, 911@param#value, 910@param#objectExample)
	if result != 2 {
	    t.Errorf("test_ReadFromArrayField2 failed")
	}
	
}

func test_DefaultValueForSuperclassFields() {

	// Test case 1
	914@param#o := 1
    result := DefaultValueForSuperclassFields(914@param#o)
	if result != G*NAMED:ObjectWithPrimitivesClassSucc {
	    t.Errorf("test_DefaultValueForSuperclassFields1 failed")
	}
	
}

func test_String() {

	// Test case 1
	927@param#o := 1
    result := String(927@param#o)
	if result != "out of bounds" {
	    t.Errorf("test_String1 failed")
	}
	
}

func test_ValueByDefault() {

	// Test case 1
	930@param#o := 2
    result := ValueByDefault(930@param#o)
	if result != 5 {
	    t.Errorf("test_ValueByDefault1 failed")
	}
	
}

func test_CompareTwoObjectsWithTheDifferentRefField() {

	// Test case 1
	943@param#snd := 3
	942@param#fst := 1
	936@param#o := 2
    result := CompareTwoObjectsWithTheDifferentRefField(943@param#snd, 942@param#fst, 936@param#o)
	if result != false {
	    t.Errorf("test_CompareTwoObjectsWithTheDifferentRefField1 failed")
	}
	
}

func test_ReadFromRefTypeField() {

	// Test case 1
	946@param#o := 2
	952@param#objectExample := 1
    result := ReadFromRefTypeField(946@param#o, 952@param#objectExample)
	if result != 4301 {
	    t.Errorf("test_ReadFromRefTypeField1 failed")
	}
	
	// Test case 2
	946@param#o := 1
	952@param#objectExample := 1
    result := ReadFromRefTypeField(946@param#o, 952@param#objectExample)
	if result != -1 {
	    t.Errorf("test_ReadFromRefTypeField2 failed")
	}
	
}

func test_WriteToArrayField() {

	// Test case 1
	957@param#o := 1
	964@param#length := 3
	963@param#objectExample := 2
    result := WriteToArrayField(957@param#o, 964@param#length, 963@param#objectExample)
	if result != G*NAMED:ObjectWithRefFieldClass, string!val!96784904 {
	    t.Errorf("test_WriteToArrayField1 failed")
	}
	
	// Test case 2
	957@param#o := 1
	964@param#length := 1
	963@param#objectExample := 1
    result := WriteToArrayField(957@param#o, 964@param#length, 963@param#objectExample)
	if result != nil, string!val!-1565175336 {
	    t.Errorf("test_WriteToArrayField2 failed")
	}
	
}

func test_CreateObject() {

	// Test case 1
	971@param#b := -9223372036854775808
	969@param#o := 1
	970@param#a := 1
	977@param#objectExample := 1
    result := CreateObject(971@param#b, 969@param#o, 970@param#a, 977@param#objectExample)
	if result != nil, string!val!-1565175336 {
	    t.Errorf("test_CreateObject1 failed")
	}
	
	// Test case 2
	971@param#b := 26094
	969@param#o := 1
	970@param#a := 0
	977@param#objectExample := 1
    result := CreateObject(971@param#b, 969@param#o, 970@param#a, 977@param#objectExample)
	if result != G*NAMED:ObjectWithPrimitivesClass, string!val!96784904 {
	    t.Errorf("test_CreateObject2 failed")
	}
	
}

func test_NextValue() {

	// Test case 1
	980@param#node := 1
	978@param#r := 1
	981@param#value := 1
    result := NextValue(980@param#node, 978@param#r, 981@param#value)
	if result != G*NAMED:RecursiveTypeClass, string!val!96784904 {
	    t.Errorf("test_NextValue1 failed")
	}
	
	// Test case 2
	980@param#node := 1
	978@param#r := 1
	981@param#value := -9223372036854775808
    result := NextValue(980@param#node, 978@param#r, 981@param#value)
	if result != nil, string!val!96784904 {
	    t.Errorf("test_NextValue2 failed")
	}
	
	// Test case 3
	980@param#node := 2
	978@param#r := 1
	981@param#value := 0
    result := NextValue(980@param#node, 978@param#r, 981@param#value)
	if result != nil, string!val!-1565175336 {
	    t.Errorf("test_NextValue3 failed")
	}
	
}

func test_CompareTwoObjectsWithDifferentRefField() {

	// Test case 1
	992@param#snd := 1
	993@param#value := 6256100889573727065
	991@param#fst := 1
	985@param#o := 1
    result := CompareTwoObjectsWithDifferentRefField(992@param#snd, 993@param#value, 991@param#fst, 985@param#o)
	if result != 1 {
	    t.Errorf("test_CompareTwoObjectsWithDifferentRefField1 failed")
	}
	
	// Test case 2
	992@param#snd := 1
	993@param#value := 0
	991@param#fst := 2
	985@param#o := 2
    result := CompareTwoObjectsWithDifferentRefField(992@param#snd, 993@param#value, 991@param#fst, 985@param#o)
	if result != 2 {
	    t.Errorf("test_CompareTwoObjectsWithDifferentRefField2 failed")
	}
	
}

func test_WriteToRefTypeField() {

	// Test case 1
	1008@param#value := 42
	1007@param#objectExample := 2
	1001@param#o := 1
    result := WriteToRefTypeField(1008@param#value, 1007@param#objectExample, 1001@param#o)
	if result != nil, string!val!-1565175336 {
	    t.Errorf("test_WriteToRefTypeField1 failed")
	}
	
	// Test case 2
	1008@param#value := -1
	1001@param#o := 1
	1007@param#objectExample := 2
    result := WriteToRefTypeField(1008@param#value, 1001@param#o, 1007@param#objectExample)
	if result != nil, string!val!-1565175336 {
	    t.Errorf("test_WriteToRefTypeField2 failed")
	}
	
}

func test_CompareTwoOuterObjects() {

	// Test case 1
	1010@param#o := 1
	1017@param#snd := 2
	1016@param#fst := 1
    result := CompareTwoOuterObjects(1010@param#o, 1017@param#snd, 1016@param#fst)
	if result != false {
	    t.Errorf("test_CompareTwoOuterObjects1 failed")
	}
	
}

func test_CreateWithConstructor() {

	// Test case 1
	1019@param#x := 2
	1020@param#y := 1
	1018@param#o := 2
    result := CreateWithConstructor(1019@param#x, 1020@param#y, 1018@param#o)
	if result != G*NAMED:ObjectWithPrimitivesClass {
	    t.Errorf("test_CreateWithConstructor1 failed")
	}
	
}

func test_GetOrDefault() {

	// Test case 1
	1026@param#o := 1
	1033@param#defaultValue := 1
	1032@param#objectExample := 2
    result := GetOrDefault(1026@param#o, 1033@param#defaultValue, 1032@param#objectExample)
	if result != G*NAMED:ObjectWithPrimitivesClass, string!val!96784904 {
	    t.Errorf("test_GetOrDefault1 failed")
	}
	
	// Test case 2
	1026@param#o := 2
	1033@param#defaultValue := 2
	1032@param#objectExample := 1
    result := GetOrDefault(1026@param#o, 1033@param#defaultValue, 1032@param#objectExample)
	if result != nil, string!val!-1565175336 {
	    t.Errorf("test_GetOrDefault2 failed")
	}
	
	// Test case 3
	1026@param#o := 1
	1033@param#defaultValue := 1
	1032@param#objectExample := 2
    result := GetOrDefault(1026@param#o, 1033@param#defaultValue, 1032@param#objectExample)
	if result != G*NAMED:ObjectWithPrimitivesClass, string!val!96784904 {
	    t.Errorf("test_GetOrDefault3 failed")
	}
	
}

func test_CompareObjectWithArgument() {

	// Test case 1
	1034@param#o := 1
	1040@param#fst := 2
    result := CompareObjectWithArgument(1034@param#o, 1040@param#fst)
	if result != 1 {
	    t.Errorf("test_CompareObjectWithArgument1 failed")
	}
	
}

func test_CreateWithSuperConstructor() {

	// Test case 1
	1043@param#y := 1
	1041@param#o := 2
	1042@param#x := 2338556819616863370
	1044@param#anotherX := 0
    result := CreateWithSuperConstructor(1043@param#y, 1041@param#o, 1042@param#x, 1044@param#anotherX)
	if result != G*NAMED:ObjectWithPrimitivesClassSucc {
	    t.Errorf("test_CreateWithSuperConstructor1 failed")
	}
	
}

func test_IgnoredInputParameters() {

	// Test case 1
	1057@param#fst := 2
	1051@param#o := 2
	1058@param#snd := 1
    result := IgnoredInputParameters(1057@param#fst, 1051@param#o, 1058@param#snd)
	if result != G*NAMED:ObjectWithPrimitivesClass {
	    t.Errorf("test_IgnoredInputParameters1 failed")
	}
	
}

func test_DefaultFieldValues() {

	// Test case 1
	1059@param#o := 2
    result := DefaultFieldValues(1059@param#o)
	if result != G*NAMED:ObjectWithRefFieldClass {
	    t.Errorf("test_DefaultFieldValues1 failed")
	}
	
}

func test_Example() {

	// Test case 1
	1065@param#o := 1
	1071@param#value := 1
    result := Example(1065@param#o, 1071@param#value)
	if result != G*NAMED:ObjectWithPrimitivesClass {
	    t.Errorf("test_Example1 failed")
	}
	
	// Test case 2
	1065@param#o := 2
	1071@param#value := 2
    result := Example(1065@param#o, 1071@param#value)
	if result != G*NAMED:ObjectWithPrimitivesClass {
	    t.Errorf("test_Example2 failed")
	}
	
}

func test_Max() {

	// Test case 1
	1072@param#o := 2
	1079@param#snd := 3
	1078@param#fst := 2
    result := Max(1072@param#o, 1079@param#snd, 1078@param#fst)
	if result != G*NAMED:ObjectWithPrimitivesClass {
	    t.Errorf("test_Max1 failed")
	}
	
}

func test_NullExample() {

	// Test case 1
	1086@param#object := 1
	1080@param#o := 2
    result := NullExample(1086@param#object, 1080@param#o)
	if result != nil {
	    t.Errorf("test_NullExample1 failed")
	}
	
	// Test case 2
	1086@param#object := 2
	1080@param#o := 1
    result := NullExample(1086@param#object, 1080@param#o)
	if result != G*NAMED:ObjectWithPrimitivesClass {
	    t.Errorf("test_NullExample2 failed")
	}
	
	// Test case 3
	1086@param#object := 1
	1080@param#o := 2
    result := NullExample(1086@param#object, 1080@param#o)
	if result != G*NAMED:ObjectWithPrimitivesClass {
	    t.Errorf("test_NullExample3 failed")
	}
	
}

func main() {
	test_CompareTwoDifferentObjectsFromArguments()
	test_DefaultValue()
	test_Equals()
	test_CompareTwoIdenticalObjectsFromArguments()
	test_WriteObjectField()
	test_CompareTwoRefEqualObjects()
	test_CompareTwoObjectsWithNullRefField()
	test_FieldWithDefaultValue()
	test_Memory()
	test_CompareTwoObjectsWithTheSameRefField()
	test_CompareTwoDifferentObjects()
	test_CompareTwoNullObjects()
	test_InheritorsFields()
	test_ReadFromArrayField()
	test_DefaultValueForSuperclassFields()
	test_String()
	test_ValueByDefault()
	test_CompareTwoObjectsWithTheDifferentRefField()
	test_ReadFromRefTypeField()
	test_WriteToArrayField()
	test_CreateObject()
	test_NextValue()
	test_CompareTwoObjectsWithDifferentRefField()
	test_WriteToRefTypeField()
	test_CompareTwoOuterObjects()
	test_CreateWithConstructor()
	test_GetOrDefault()
	test_CompareObjectWithArgument()
	test_CreateWithSuperConstructor()
	test_IgnoredInputParameters()
	test_DefaultFieldValues()
	test_Example()
	test_Max()
	test_NullExample()
}

// Original code:

import "errors"

type RecursiveTypeClass struct {
	Next  *RecursiveTypeClass
	Value int
}

type RecursiveType struct{}

func (r *RecursiveType) NextValue(node *RecursiveTypeClass, value int) (*RecursiveTypeClass, error) {
	if value == 0 {
		return nil, errors.New("IllegalArgumentException: value cannot be 0")
	}
	if node.Next != nil && node.Next.Value == value {
		return node.Next, nil
	}
	return nil, nil
}

func (r *RecursiveType) WriteObjectField(node *RecursiveTypeClass) *RecursiveTypeClass {
	if node.Next == nil {
		node.Next = &RecursiveTypeClass{}
	}
	node.Next.Value = node.Next.Value + 1
	return node
}


type ObjectWithPrimitivesClass struct {
	ValueByDefault int
	x, y           int
	ShortValue     int16
	Weight         float64
}

func NewObjectWithPrimitivesClass() *ObjectWithPrimitivesClass {
	return &ObjectWithPrimitivesClass{ValueByDefault: 5}
}

func NewObjectWithPrimitivesClassWithParams(x, y int, weight float64) *ObjectWithPrimitivesClass {
	return &ObjectWithPrimitivesClass{
		ValueByDefault: 5,
		x:              x,
		y:              y,
		Weight:         weight,
	}
}

func (o *ObjectWithPrimitivesClass) Equals(other *ObjectWithPrimitivesClass) bool {
	if other == nil {
		return false
	}
	return o.ValueByDefault == other.ValueByDefault && o.x == other.x && o.y == other.y && o.Weight == other.Weight
}

func (o *ObjectWithPrimitivesClass) String() string {
	return fmt.Sprintf(
		"ObjectWithPrimitivesClass(ValueByDefault = %d, x = %d, y = %d, ShortValue = %d, Weight = %f)",
		o.ValueByDefault,
		o.x,
		o.y,
		o.ShortValue,
		o.Weight,
	)
}

type ObjectWithPrimitivesExample struct{}

func (o *ObjectWithPrimitivesExample) Max(fst, snd *ObjectWithPrimitivesClass) *ObjectWithPrimitivesClass {
	if fst.x > snd.x && fst.y > snd.y {
		return fst
	} else if fst.x < snd.x && fst.y < snd.y {
		return snd
	}
	return fst
}

func (o *ObjectWithPrimitivesExample) IgnoredInputParameters(fst, snd *ObjectWithPrimitivesClass) *ObjectWithPrimitivesClass {
	return NewObjectWithPrimitivesClass()
}

func (o *ObjectWithPrimitivesExample) Example(value *ObjectWithPrimitivesClass) *ObjectWithPrimitivesClass {
	if value.x == 1 {
		return value
	}
	value.x = 1
	return value
}

func (o *ObjectWithPrimitivesExample) DefaultValueForSuperclassFields() *ObjectWithPrimitivesClassSucc {
	obj := &ObjectWithPrimitivesClassSucc{}
	if obj.x != 0 {
		return obj
	}
	return obj
}

func (o *ObjectWithPrimitivesExample) CreateObject(a, b int, objectExample *ObjectWithPrimitivesClass) (*ObjectWithPrimitivesClass, error) {
	object := NewObjectWithPrimitivesClass()
	object.x = a + 5
	object.y = b + 6
	object.Weight = objectExample.Weight
	if object.Weight < 0 {
		return nil, errors.New("IllegalArgumentException: weight < 0")
	}
	return object, nil
}

func (o *ObjectWithPrimitivesExample) Memory(objectExample *ObjectWithPrimitivesClass, value int) *ObjectWithPrimitivesClass {
	if value > 0 {
		objectExample.x = 1
		objectExample.y = 2
		objectExample.Weight = 1.2
	} else {
		objectExample.x = -1
		objectExample.y = -2
		objectExample.Weight = -1.2
	}
	return objectExample
}

func (o *ObjectWithPrimitivesExample) NullExample(object *ObjectWithPrimitivesClass) *ObjectWithPrimitivesClass {
	if object.x == 0 && object.y == 0 {
		return nil
	}
	return object
}

func (o *ObjectWithPrimitivesExample) CompareTwoNullObjects(value int) int {
	fst := NewObjectWithPrimitivesClass()
	snd := NewObjectWithPrimitivesClass()

	fst.x = value + 1
	snd.x = value + 2

	if fst.x == value+1 {
		fst = nil
	}
	if snd.x == value+2 {
		snd = nil
	}

	if fst == snd {
		return 1
	}
	panic("RuntimeException")
}

func (o *ObjectWithPrimitivesExample) CompareTwoOuterObjects(fst, snd *ObjectWithPrimitivesClass) bool {
	if fst == nil || snd == nil {
		panic("NullPointerException")
	}
	return fst == snd
}

func (o *ObjectWithPrimitivesExample) CompareTwoDifferentObjects(a int) int {
	fst := NewObjectWithPrimitivesClass()
	snd := NewObjectWithPrimitivesClass()

	fst.x = a
	snd.x = a

	if fst == snd {
		panic("RuntimeException")
	}
	return 1
}

func (o *ObjectWithPrimitivesExample) CompareTwoRefEqualObjects(a int) int {
	fst := NewObjectWithPrimitivesClass()
	snd := fst

	fst.x = a

	if fst == snd {
		return 1
	}
	panic("RuntimeException")
}

func (o *ObjectWithPrimitivesExample) CompareObjectWithArgument(fst *ObjectWithPrimitivesClass) int {
	snd := NewObjectWithPrimitivesClass()

	if snd == fst {
		panic("RuntimeException")
	}
	return 1
}

func (o *ObjectWithPrimitivesExample) CompareTwoIdenticalObjectsFromArguments(fst, snd *ObjectWithPrimitivesClass) int {
	fst.x = snd.x
	fst.y = snd.y
	fst.Weight = snd.Weight

	if fst == snd {
		return 1
	}
	return 2
}

func (o *ObjectWithPrimitivesExample) GetOrDefault(objectExample, defaultValue *ObjectWithPrimitivesClass) (*ObjectWithPrimitivesClass, error) {
	if defaultValue.x == 0 && defaultValue.y == 0 {
		return nil, errors.New("IllegalArgumentException")
	}
	if objectExample == nil {
		return defaultValue, nil
	}
	return objectExample, nil
}

func (o *ObjectWithPrimitivesExample) InheritorsFields(fst *ObjectWithPrimitivesClassSucc, snd *ObjectWithPrimitivesClass) int {
	fst.x = 1
	fst.AnotherX = 2
	fst.y = 3
	fst.Weight = 4.5

	snd.x = 1
	snd.y = 2
	snd.Weight = 3.4

	return 1
}

func (o *ObjectWithPrimitivesExample) CreateWithConstructor(x, y int) *ObjectWithPrimitivesClass {
	return NewObjectWithPrimitivesClassWithParams(x+1, y+2, 3.3)
}

func (o *ObjectWithPrimitivesExample) CreateWithSuperConstructor(x, y, anotherX int) *ObjectWithPrimitivesClassSucc {
	return &ObjectWithPrimitivesClassSucc{
		ObjectWithPrimitivesClass: ObjectWithPrimitivesClass{
			ValueByDefault: 5,
			x:              x + 1,
			y:              y + 2,
			Weight:         3.3,
		},
		AnotherX: anotherX + 4,
	}
}

func (o *ObjectWithPrimitivesExample) FieldWithDefaultValue(x, y int) *ObjectWithPrimitivesClass {
	return NewObjectWithPrimitivesClassWithParams(x, y, 3.3)
}

func (o *ObjectWithPrimitivesExample) ValueByDefault() int {
	objectExample := NewObjectWithPrimitivesClass()
	return objectExample.ValueByDefault
}

type ObjectWithPrimitivesClassSucc struct {
	ObjectWithPrimitivesClass
	AnotherX int
}

import "errors"

type SimpleDataClass struct {
	a int
	b int
}

type ObjectWithRefFieldClass struct {
	x, y       int
	Weight     float64
	RefField   *SimpleDataClass
	ArrayField []int
}

type ObjectWithRefFieldExample struct{}

func (o *ObjectWithRefFieldExample) DefaultValue() *ObjectWithRefFieldClass {
	obj := &ObjectWithRefFieldClass{}
	if obj.x != 0 || obj.y != 0 || obj.Weight != 0.0 || obj.RefField != nil || obj.ArrayField != nil {
		return obj
	}
	return obj
}

func (o *ObjectWithRefFieldExample) WriteToRefTypeField(objectExample *ObjectWithRefFieldClass, value int) (*ObjectWithRefFieldClass, error) {
	if value != 42 {
		return nil, errors.New("IllegalArgumentException: value must be 42")
	} else if objectExample.RefField != nil {
		return nil, errors.New("IllegalArgumentException: refField must be nil")
	}

	simpleDataClass := &SimpleDataClass{
		a: value,
		b: value * 2,
	}
	objectExample.RefField = simpleDataClass
	return objectExample, nil
}

func (o *ObjectWithRefFieldExample) DefaultFieldValues() *ObjectWithRefFieldClass {
	return &ObjectWithRefFieldClass{}
}

func (o *ObjectWithRefFieldExample) ReadFromRefTypeField(objectExample *ObjectWithRefFieldClass) int {
	if objectExample.RefField == nil || objectExample.RefField.a <= 0 {
		return -1
	}
	return objectExample.RefField.a
}

func (o *ObjectWithRefFieldExample) WriteToArrayField(objectExample *ObjectWithRefFieldClass, length int) (*ObjectWithRefFieldClass, error) {
	if length < 3 {
		return nil, errors.New("IllegalArgumentException: length must be at least 3")
	}

	array := make([]int, length)
	for i := 0; i < length; i++ {
		array[i] = i + 1
	}

	objectExample.ArrayField = array
	objectExample.ArrayField[length-1] = 100

	return objectExample, nil
}

func (o *ObjectWithRefFieldExample) ReadFromArrayField(objectExample *ObjectWithRefFieldClass, value int) int {
	if len(objectExample.ArrayField) > 2 && objectExample.ArrayField[2] == value {
		return 1
	}
	return 2
}

func (o *ObjectWithRefFieldExample) CompareTwoDifferentObjectsFromArguments(fst, snd *ObjectWithRefFieldClass) int {
	if fst.x > 0 && snd.x < 0 {
		if fst == snd {
			panic("RuntimeException")
		} else {
			return 1
		}
	}

	fst.x = snd.x
	fst.y = snd.y
	fst.Weight = snd.Weight

	if fst == snd {
		return 2
	}

	return 3
}

func (o *ObjectWithRefFieldExample) CompareTwoObjectsWithNullRefField(fst, snd *ObjectWithRefFieldClass) int {
	fst.RefField = nil
	snd.RefField = &SimpleDataClass{}
	if fst == snd {
		return 1
	}
	return 2
}

func (o *ObjectWithRefFieldExample) CompareTwoObjectsWithDifferentRefField(fst, snd *ObjectWithRefFieldClass, value int) int {
	fst.RefField = &SimpleDataClass{a: value}
	snd.RefField = &SimpleDataClass{a: fst.RefField.a + 1}
	fst.RefField.b = snd.RefField.b

	if fst == snd {
		return 1
	}
	return 2
}

func (o *ObjectWithRefFieldExample) CompareTwoObjectsWithTheDifferentRefField(fst, snd *ObjectWithRefFieldClass) bool {
	return fst.RefField == snd.RefField
}

func (o *ObjectWithRefFieldExample) CompareTwoObjectsWithTheSameRefField(fst, snd *ObjectWithRefFieldClass) int {
	simpleDataClass := &SimpleDataClass{}

	fst.RefField = simpleDataClass
	snd.RefField = simpleDataClass
	fst.x = snd.x
	fst.y = snd.y
	fst.Weight = snd.Weight

	if fst == snd {
		return 1
	}
	return 2
}

