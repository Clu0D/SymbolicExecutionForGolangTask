package main

import (
    "encoding/json"
    "flag"
    "fmt"
    "go/ast"
    "go/parser"
    "go/token"
    "io/fs"
    "io"
    "os"
    "bytes"
    "path/filepath"
)

type NodeData struct {
    Type string `json:"type"`
    ID   int    `json:"id"`
}

type FileNode struct {
    NodeData
    Decl []interface{} `json:"decl"`
}

type IdentNode struct {
    NodeData
    Name string `json:"name"`
}

type FuncDeclNode struct {
    NodeData
    Name string `json:"name"`
    Body interface{} `json:"body"`
    Recv interface{} `json:"recv"`
    Params interface{} `json:"params"`
    Results interface{} `json:"results"`
}

type IfNode struct {
    NodeData
    Cond interface{} `json:"cond"`
    Body interface{} `json:"body"`
    ElseBody interface{} `json:"elseBody"`
}

type BinaryExprNode struct {
    NodeData
    X interface{} `json:"x"`
    Op string `json:"op"`
    Y interface{} `json:"y"`
}

type BlockStmtNode struct {
    NodeData
    Statements []interface{} `json:"statements"`
}

type ReturnStmtNode struct {
    NodeData
    Result []interface{} `json:"result"`
}

type CallExprNode struct {
    NodeData
    Call interface{} `json:"call"`
    Args []interface{} `json:"args"`
}

type BasicLitNode struct {
    NodeData
    Kind string `json:"kind"`
    Value string `json:"value"`
}

type GenDeclNode struct {
    NodeData
    Token string `json:"token"`
    Specs []SpecNode `json:"specs"`
}

type ValueSpecNode struct {
    NodeData
    Names   []string `json:"names"`
    TypeNode    interface{}   `json:"typeNode"`
    Values  []string `json:"values"`
}

type ImportSpecNode struct {
    NodeData
    Path    string `json:"path"`
}

type TypeSpecNode struct {
    NodeData
    Name    string `json:"name"`
    TypeNode interface{} `json:"typeNode"`
}

type StructTypeNode struct {
    NodeData
    Fields     interface{} `json:"fields"`
    Incomplete bool         `json:"incomplete"`
}

type RangeStmtNode struct {
    NodeData
    Key       interface{}      `json:"key"`
    Value     interface{}      `json:"value"`
    Tok       string           `json:"tok"`
    X         interface{}      `json:"x"`
    Body      interface{}      `json:"body"`
}

type SpecNode interface {
    isSpecNode()
}

func (v ValueSpecNode) isSpecNode() {}
func (i ImportSpecNode) isSpecNode() {}
func (t TypeSpecNode) isSpecNode() {}


type AssignStmtNode struct {
    NodeData
    Lhs     []interface{} `json:"lhs"`
    Rhs     []interface{} `json:"rhs"`
    Token   string        `json:"token"`
}

type FieldNode struct {
    Names   []string `json:"names"`
    TypeName  interface{} `json:"typeName"`
}

type ForNode struct {
    NodeData
    Init    interface{} `json:"init"`
    Cond    interface{} `json:"cond"`
    Post    interface{} `json:"post"`
    Body    interface{} `json:"body"`
}

type IncDecNode struct {
    NodeData
    X       interface{} `json:"x"`
    Tok     string `json:"tok"`
}

type UnaryExprNode struct {
    NodeData
    Op      string `json:"op"`
    X       interface{} `json:"x"`
}

type IndexExprNode struct {
    NodeData
    X      interface{} `json:"x"`
    Index  interface{} `json:"index"`
}

type ArrayTypeNode struct {
    NodeData
    Len     interface{} `json:"length"`
    Elt     interface{} `json:"elementType"`
}

type StarExprNode struct {
    NodeData
    X      interface{} `json:"x"`
}

type SelectorExprNode struct {
    NodeData
    X      interface{} `json:"x"`
    Selector interface{} `json:"selector"`
}

type Unsupported struct {
    NodeData
    Unsupported string `json:"unsupported"`
}

func encodeDeclarations(decls []ast.Decl, nodeIDCounter *int) []interface{} {
    var encodedDecls []interface{}
    for _, decl := range decls {
        encodedDecl := encodeNode(decl, nodeIDCounter)
        encodedDecls = append(encodedDecls, encodedDecl)
    }
    return encodedDecls
}

func encodeArgs(args []ast.Expr, nodeIDCounter *int) []interface{} {
    var encodedArgs []interface{}
    for _, arg := range args {
        encodedArg := encodeNode(arg, nodeIDCounter)
        encodedArgs = append(encodedArgs, encodedArg)
    }
    return encodedArgs
}

func convertSpecs(specs []ast.Spec, nodeIDCounter *int) []SpecNode {
    var specNodes []SpecNode

    for _, spec := range specs {
        id := *nodeIDCounter
        *nodeIDCounter++
        switch s := spec.(type) {
            case *ast.ImportSpec:
                specNodes = append(specNodes, ImportSpecNode{NodeData{fmt.Sprintf("%T", spec), id}, s.Path.Value})
            case *ast.ValueSpec:
                var names []string
                for _, name := range s.Names {
                    names = append(names, name.Name)
                }
                typeNode := encodeNode(s.Type, nodeIDCounter)
                var values []string
                for _, value := range s.Values {
                    values = append(values, fmt.Sprintf("%v", value))
                }
                specNodes = append(specNodes, ValueSpecNode{NodeData{fmt.Sprintf("%T", spec), id}, names, typeNode, values})
            case *ast.TypeSpec:
                typeNode := encodeNode(s.Type, nodeIDCounter)
                specNodes = append(specNodes, TypeSpecNode{NodeData{fmt.Sprintf("%T", spec), id}, s.Name.Name, typeNode})
        }
        id++
    }

    return specNodes
}

func encodeNode(node ast.Node, nodeIDCounter *int) (interface{}) {
    if node == nil {
        return nil
    }

    id := *nodeIDCounter
    *nodeIDCounter++

    switch n := node.(type) {

    case *ast.File:
        return FileNode{NodeData{fmt.Sprintf("%T", node), id}, encodeDeclarations(n.Decls, nodeIDCounter)}

    case *ast.Ident:
        return IdentNode{NodeData{fmt.Sprintf("%T", node), id}, n.Name}

    case *ast.FuncDecl:
        return FuncDeclNode{
            NodeData{fmt.Sprintf("%T", node), id},
            n.Name.Name,
            encodeNode(n.Body, nodeIDCounter),
            encodeNode(n.Recv, nodeIDCounter),
            encodeNode(n.Type.Params, nodeIDCounter),
            encodeNode(n.Type.Results, nodeIDCounter),
        }

    case *ast.BlockStmt:
        var encodedList []interface{}
        for _, stmt := range n.List {
            encodedStmt := encodeNode(stmt, nodeIDCounter)
            encodedList = append(encodedList, encodedStmt)
        }
        return BlockStmtNode{NodeData{fmt.Sprintf("%T", node), id}, encodedList}

    case *ast.ReturnStmt:
        var encodedResults []interface{}
        for _, result := range n.Results {
            encodedResult := encodeNode(result, nodeIDCounter)
            encodedResults = append(encodedResults, encodedResult)
        }
        return ReturnStmtNode{NodeData{fmt.Sprintf("%T", node), id}, encodedResults}

    case *ast.AssignStmt:
        var encodedLHS []interface{}
        for _, lhs := range n.Lhs {
            encodedLHS = append(encodedLHS, encodeNode(lhs, nodeIDCounter))
        }
        var encodedRHS []interface{}
        for _, rhs := range n.Rhs {
            encodedRHS = append(encodedRHS, encodeNode(rhs, nodeIDCounter))
        }
        return AssignStmtNode{
            NodeData{fmt.Sprintf("%T", node), id},
            encodedLHS,
            encodedRHS,
            n.Tok.String(),
        }

    case *ast.FieldList:
        var fields []FieldNode
        // todo huh why it fails here
        if n != nil {
            for _, field := range n.List {
                var names = make([]string, len(field.Names))
                for i, name := range field.Names {
                    names[i] = name.Name
                }
                var fieldType = encodeNode(field.Type, nodeIDCounter)
                fields = append(fields, FieldNode{names,fieldType})
            }
        }
        return fields

    case *ast.UnaryExpr:
        return UnaryExprNode{
            NodeData{fmt.Sprintf("%T", node), id},
            n.Op.String(),
            encodeNode(n.X, nodeIDCounter),
        }


    case *ast.CallExpr:
        return CallExprNode{
            NodeData{fmt.Sprintf("%T", node), id},
            encodeNode(n.Fun, nodeIDCounter),
            encodeArgs(n.Args, nodeIDCounter),
        }

    case *ast.IfStmt:
        return IfNode{
            NodeData{fmt.Sprintf("%T", node), id},
            encodeNode(n.Cond, nodeIDCounter),
            encodeNode(n.Body, nodeIDCounter),
            encodeNode(n.Else, nodeIDCounter),
        }

    case *ast.BinaryExpr:
        return BinaryExprNode{
            NodeData{fmt.Sprintf("%T", node), id},
            encodeNode(n.X, nodeIDCounter),
            n.Op.String(),
            encodeNode(n.Y, nodeIDCounter),
        }

    case *ast.BasicLit:
        return BasicLitNode{
            NodeData{fmt.Sprintf("%T", node), id},
            n.Kind.String(),
            n.Value,
        }

    case *ast.DeclStmt:
        return encodeNode(n.Decl, nodeIDCounter)

    case *ast.GenDecl:
        return GenDeclNode{
            NodeData{fmt.Sprintf("%T", node), id},
            n.Tok.String(),
            convertSpecs(n.Specs, nodeIDCounter),
        }

    case *ast.ForStmt:
        return ForNode{
            NodeData{fmt.Sprintf("%T", node), id},
            encodeNode(n.Init, nodeIDCounter),
            encodeNode(n.Cond, nodeIDCounter),
            encodeNode(n.Post, nodeIDCounter),
            encodeNode(n.Body, nodeIDCounter),
        }

    case *ast.IncDecStmt:
        return IncDecNode{
            NodeData{fmt.Sprintf("%T", node), id},
            encodeNode(n.X, nodeIDCounter),
            n.Tok.String(),
        }

    case *ast.ParenExpr:
        return encodeNode(n.X, nodeIDCounter)

    case *ast.IndexExpr:
        return IndexExprNode{
            NodeData{fmt.Sprintf("%T", node), id},
            encodeNode(n.X, nodeIDCounter),
            encodeNode(n.Index, nodeIDCounter),
        }

    case *ast.ArrayType:
        return ArrayTypeNode{
            NodeData{fmt.Sprintf("%T", node), id},
            encodeNode(n.Len, nodeIDCounter),
            encodeNode(n.Elt, nodeIDCounter),
        }

    case *ast.TypeSpec:
        return TypeSpecNode{
            NodeData{fmt.Sprintf("%T", node), id},
            n.Name.Name,
            encodeNode(n.Type, nodeIDCounter),
        }

    case *ast.StarExpr:
        return StarExprNode{
            NodeData{fmt.Sprintf("%T", node), id},
            encodeNode(n.X, nodeIDCounter),
        }

    case *ast.SelectorExpr:
        return SelectorExprNode{
            NodeData{fmt.Sprintf("%T", node), id},
            encodeNode(n.X, nodeIDCounter),
            encodeNode(n.Sel, nodeIDCounter),
        }

    case *ast.StructType:
        return StructTypeNode{
            NodeData{fmt.Sprintf("%T", node), id},
            encodeNode(n.Fields, nodeIDCounter),
            n.Incomplete,
        }

    case *ast.RangeStmt:
        return RangeStmtNode{
            NodeData{fmt.Sprintf("%T", node), id},
           encodeNode(n.Key, nodeIDCounter),
           encodeNode(n.Value, nodeIDCounter),
            n.Tok.String(),
           encodeNode(n.X, nodeIDCounter),
           encodeNode(n.Body, nodeIDCounter),
        }

    default:
        return Unsupported{NodeData{"Unsupported", id}, fmt.Sprintf("%T", node)}
    }

    //   nodeMap[id] = data
    return Unsupported{NodeData{"Unsupported", id}, fmt.Sprintf("%T", node)}
}

func EncodeAST(file *ast.File) ([]byte, error) {
    nodeIDCounter := 1

    encodedFile := encodeNode(file, &nodeIDCounter)

    return json.MarshalIndent(encodedFile, "", "    ")
}

func main() {
    inputDirPtr := flag.String("input", "", "Directory containing .go files to parse")
    outputDirPtr := flag.String("output", "", "Directory to save JSON AST files")

    flag.Parse()

    inputDir := *inputDirPtr
    outputDir := *outputDirPtr

    if inputDir == "" || outputDir == "" {
        fmt.Println("Please specify input and output directories using -input and -output flags.")
        flag.Usage()
        return
    }

    err := os.MkdirAll(outputDir, os.ModePerm)
    if err != nil {
        fmt.Printf("Error creating output directory: %v\n", err)
        return
    }

    filepath.Walk(inputDir, func(path string, info fs.FileInfo, err error) error {
        if err != nil {
            return err
        }

        if !info.IsDir() && filepath.Ext(path) == ".go" {
            fset := token.NewFileSet()
            node, err := parser.ParseFile(fset, path, nil, parser.ParseComments)
            if err != nil {
                fmt.Printf("Error parsing file %s: %v\n", path, err)
                return nil
            }

            relPath, err := filepath.Rel(inputDir, path)
            if err != nil {
                fmt.Printf("Error getting relative path for %s: %v\n", path, err)
                return nil
            }

            outputFileName := filepath.Join(outputDir, relPath+".json")
            fmt.Printf("Filepath %s\n", outputFileName)
            fmt.Printf("\n")
            ast.Print(fset, node)

            jsonFile, err := os.Create(outputFileName)
            if err != nil {
                fmt.Printf("Error creating file: %v\n", err)
            }
            defer jsonFile.Close()

            jsonBytes, err := EncodeAST(node)
            if err != nil {
                fmt.Printf("Error encoding file: %v\n", err)
            }

            fmt.Println(string(jsonBytes))

            _, err = io.Copy(jsonFile, bytes.NewReader(jsonBytes))
            if err != nil {
                fmt.Printf("Error encoding: %v\n", err)
            }

            fmt.Printf("Successfully Encoded: %s\n", outputFileName)
        }
        return nil
    })
}