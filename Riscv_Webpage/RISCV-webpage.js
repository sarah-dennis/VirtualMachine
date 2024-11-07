var imageSet = ["risc-v_Images/CodeArea.png", "risc-v_Images/Output.png",
                "risc-v_Images/ErrorMessages.png", "risc-v_Images/Registers.png",
                "risc-v_Images/Memory.png", "risc-v_Images/ProgramCounter.png",
                "risc-v_Images/SymbolTable.png", "risc-v_Images/StackPointer.png",
                "risc-v_Images/Run.png", "risc-v_Images/Step.png", "risc-v_Images/Reset.png",
                "risc-v_Images/Assemble.png", "risc-v_Images/Disassemble.png"];
var main = "risc-v_Images/Main.png";

function switchImage(n) {
  var mainImage = document.getElementById("interface");
  mainImage.src = imageSet[n-1]
}

function reset(){
  var mainImage = document.getElementById("interface");
  mainImage.src = main;
}
