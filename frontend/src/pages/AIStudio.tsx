import {
  BookOpen,
  Upload,
  Brain,
  Check,
  ChevronDown,
  FileText,
  Layers,
  Loader2,
  MessageSquare,
  NotepadText,
  Search,
  Sparkles,
  WandSparkles,
  X,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import type { MaterialSummary } from "../types/material";
import type { McqSetResponse } from "../types/mcq";
import type { AIModel } from "../types/ai";
import { getModelRecommendation, getModelOption, mapReasoningToBackend } from "../types/ai-models";
import ModelSelector from "../components/ai/ModelSelector";
import ReasoningSelector from "../components/ai/ReasoningSelector";
import * as materialService from "../services/materialService";
import * as mcqService from "../services/mcqService";
import api from "../services/api";
import { GenerationLimitAlert } from "../components/usage/UsageAlerts";
import { useToast } from "../components/ui";
import type { ApiResponse } from "../types/auth";

type GenerationMode = "material" | "topic" | "upload";

type AiTool = "mcq" | "summarize" | "flashcards" | "notes";

type QuizDeliveryMode = "PRACTICE" | "TIMED" | "EXAM";

const questionCounts = [5, 10, 15, 20];

const quizModes = [
  { label: "Practice", value: "PRACTICE" as const, icon: "📝", desc: "No timer, learn at your pace" },
  { label: "Timed", value: "TIMED" as const, icon: "⏱️", desc: "Countdown timer, finish on time" },
  { label: "Exam", value: "EXAM" as const, icon: "🎯", desc: "Strict mode, auto-submit" },
];

const difficulties = [
  { label: "Easy", value: "EASY" as const },
  { label: "Medium", value: "MEDIUM" as const },
  { label: "Hard", value: "HARD" as const },
];

export default function AIStudio() {
  const navigate = useNavigate();
  const toast = useToast();
  const [searchParams] = useSearchParams();

  const [selectedTool, setSelectedTool] = useState<AiTool>("mcq");
  const [mode, setMode] = useState<GenerationMode>("upload");
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState("");
  const [materials, setMaterials] = useState<MaterialSummary[]>([]);
  const [materialsLoading, setMaterialsLoading] = useState(true);
  const [selectedMaterial, setSelectedMaterial] = useState<number>(0);
  const [topicText, setTopicText] = useState("");
  const [questionCount, setQuestionCount] = useState(10);
  const [difficulty, setDifficulty] = useState<"EASY" | "MEDIUM" | "HARD">("MEDIUM");
  const [quizDeliveryMode, setQuizDeliveryMode] = useState<QuizDeliveryMode>("PRACTICE");
  const [selectedModel, setSelectedModel] = useState<string>("");
  const [reasoningEffort, setReasoningEffort] = useState<"auto" | "standard" | "deep">("auto");
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [models, setModels] = useState<AIModel[]>([]);
  const [isGenerating, setIsGenerating] = useState(false);
  const [generateError, setGenerateError] = useState("");
  const [toolResult, setToolResult] = useState<string>("");
  const [generationStage, setGenerationStage] = useState("");
  const [materialSearch, setMaterialSearch] = useState("");
  const [materialOpen, setMaterialOpen] = useState(false);
  const materialRef = useRef<HTMLDivElement>(null);
  const materialSearchRef = useRef<HTMLInputElement>(null);

  // Close material dropdown on outside click
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (materialRef.current && !materialRef.current.contains(e.target as Node)) {
        setMaterialOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, []);

  const filteredMaterials = materials.filter((m) =>
    m.title.toLowerCase().includes(materialSearch.toLowerCase())
  );

  useEffect(() => {
    materialService
      .listMaterials(0, 100)
      .then((response) => {
        const data = response.content;
        setMaterials(data);
        const urlMaterialId = searchParams.get("materialId");
        if (urlMaterialId) {
          setMode("material");
          const parsed = Number(urlMaterialId);
          if (data.some((m) => m.id === parsed)) {
            setSelectedMaterial(parsed);
          } else if (data.length > 0) {
            setSelectedMaterial(data[0].id);
          }
        } else if (data.length > 0) {
          setSelectedMaterial(data[0].id);
        }
      })
      .finally(() => setMaterialsLoading(false));

    api.get<ApiResponse<AIModel[]>>("/ai/models").then((res) => {
      setModels(res.data.data);
      const defaultModel = res.data.data.find((m) => m.isDefault);
      if (defaultModel) setSelectedModel(defaultModel.modelCode);
    });
  }, [searchParams]);

  // Close material dropdown on outside click
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (materialRef.current && !materialRef.current.contains(e.target as Node)) {
        setMaterialOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, []);

  const canGenerate =
    mode === "material" ? selectedMaterial > 0 :
    mode === "upload" ? uploadFile !== null :
    topicText.trim().length > 0;

  const stages = [
    "Connecting to AI...",
    "Analyzing content...",
    "Generating questions...",
    "Validating quality...",
    "Almost done...",
  ];

  const handleGenerate = async () => {
    if (!canGenerate) return;
    setIsGenerating(true);
    setGenerateError("");
    setToolResult("");
    setGenerationStage(stages[0]);

    // Simulate progressive stages
    let stageIndex = 0;
    const stageInterval = setInterval(() => {
      stageIndex++;
      if (stageIndex < stages.length) {
        setGenerationStage(stages[stageIndex]);
      }
    }, 3000);

    try {
      let materialIdForGenerate = mode === "material" ? selectedMaterial : undefined;

      // Handle upload mode: upload file first, then use the returned material ID
      if (mode === "upload" && uploadFile) {
        setUploading(true);
        setUploadProgress("Uploading document...");
        try {
          const uploaded = await materialService.uploadMaterial(uploadFile);
          materialIdForGenerate = uploaded.id;
          setUploadProgress("Document uploaded! Generating quiz...");
          toast.success("Document uploaded successfully!");
          // Refresh materials list
          const refreshed = await materialService.listMaterials(0, 100);
          setMaterials(refreshed.content);
        } catch (uploadErr: unknown) {
          let msg = "Upload failed";
          if (typeof uploadErr === "object" && uploadErr !== null && "response" in uploadErr) {
            const axiosErr = uploadErr as { response?: { data?: { message?: string } } };
            msg = axiosErr.response?.data?.message ?? "Upload failed";
          }
          setGenerateError(msg);
          setUploading(false);
          return;
        }
        setUploading(false);
      }

      if (selectedTool === "mcq") {
        const result: McqSetResponse = await mcqService.generateMcqs({
          ...(materialIdForGenerate
            ? { materialId: materialIdForGenerate }
            : { prompt: topicText.trim() }),
          numberOfQuestions: questionCount,
          difficulty,
          modelCode: selectedModel || undefined,
          reasoningEffort: mapReasoningToBackend(reasoningEffort, selectedModel || null),
        });
        clearInterval(stageInterval);
        setGenerationStage("Complete!");
        toast.success("Quiz generated successfully!");
        setTimeout(() => {
          navigate("/quiz", { state: { mcqSet: result, quizMode: quizDeliveryMode } });
        }, 500);
      } else {
        const body = {
          ...(mode === "material"
            ? { materialId: selectedMaterial }
            : { text: topicText.trim() }),
          count: questionCount,
          modelCode: selectedModel || undefined,
        };
        const endpoint = selectedTool === "summarize" ? "/ai/tools/summarize"
          : selectedTool === "flashcards" ? "/ai/tools/flashcards"
          : "/ai/tools/revision-notes";
        const res = await api.post(endpoint, body);
        clearInterval(stageInterval);
        setGenerationStage("Complete!");
        setToolResult(JSON.stringify(res.data.data, null, 2));
      }
    } catch (err: unknown) {
      clearInterval(stageInterval);
      setGenerationStage("");
      if (typeof err === "object" && err !== null && "response" in err) {
        const axiosErr = err as { response?: { data?: { message?: string } } };
        setGenerateError(axiosErr.response?.data?.message ?? "Failed to generate.");
      } else {
        setGenerateError("Failed to generate.");
      }
    } finally {
      setIsGenerating(false);
    }
  };

  const currentMaterial = materials.find((m) => m.id === selectedMaterial);
  const currentDifficulty = difficulties.find((d) => d.value === difficulty);

  return (
    <div className="mx-auto max-w-6xl space-y-8">
      <section>
        <div className="mb-3 inline-flex items-center gap-2 rounded-full bg-[var(--mq-primary-light)] px-3 py-1.5 text-xs font-semibold text-[var(--mq-primary)]">
          <Sparkles size={14} />
          AI Studio
        </div>
        <h1 className="text-3xl font-bold tracking-tight text-[var(--mq-text)]">Generate MCQs</h1>
        <p className="mt-2 max-w-2xl text-[var(--mq-text-secondary)]">
          Turn your learning material or any topic into personalized multiple-choice questions and test your understanding.
        </p>
      </section>

      <GenerationLimitAlert />

      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-6 shadow-[var(--mq-shadow-sm)] sm:p-8">
          <div className="flex items-center gap-3 border-b border-[var(--mq-border)] pb-6">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-[var(--mq-primary-light)] text-[var(--mq-primary)]">
              <WandSparkles size={21} />
            </div>
            <div>
              <h2 className="font-semibold text-[var(--mq-text)]">MCQ Generation</h2>
              <p className="text-sm text-[var(--mq-text-secondary)]">Configure your quiz</p>
            </div>
          </div>

          <div className="mt-7 space-y-6">
            {/* AI Tool Selector */}
            <div>
              <label className="mb-2 block text-sm font-semibold text-[var(--mq-text)]">AI Tool</label>
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                {[
                  { value: "mcq" as const, label: "MCQ Quiz", icon: Brain, available: true, desc: "Generate practice quizzes" },
                  { value: "summarize" as const, label: "Summarize", icon: FileText, available: false, desc: "Coming soon" },
                  { value: "flashcards" as const, label: "Flashcards", icon: Layers, available: false, desc: "Coming soon" },
                  { value: "notes" as const, label: "Revision Notes", icon: NotepadText, available: false, desc: "Coming soon" },
                ].map((tool) => {
                  const Icon = tool.icon;
                  const isDisabled = !tool.available;
                  return (
                    <button key={tool.value} type="button"
                      onClick={() => { if (!isDisabled) { setSelectedTool(tool.value); setToolResult(""); } }}
                      disabled={isDisabled}
                      className={`relative flex flex-col items-center gap-1.5 rounded-xl border px-3 py-3 text-xs font-medium transition ${
                        isDisabled
                          ? "cursor-not-allowed border-[var(--mq-border)] bg-[var(--mq-bg)] text-[var(--mq-text-muted)]"
                          : selectedTool === tool.value
                            ? "border-[var(--mq-primary)] bg-[var(--mq-primary-light)] text-[var(--mq-primary)]"
                            : "border-[var(--mq-border)] bg-[var(--mq-surface)] text-[var(--mq-text-secondary)] hover:border-[var(--mq-text-muted)] hover:text-[var(--mq-text)]"
                      }`}>
                      <Icon size={18} className={isDisabled ? "text-[var(--mq-text-muted)]" : ""} />
                      <span className={isDisabled ? "text-[var(--mq-text-muted)]" : ""}>{tool.label}</span>
                      {isDisabled && (
                        <span className="mt-0.5 text-[10px] font-medium text-[var(--mq-text-muted)]">Coming Soon</span>
                      )}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Generate From -- Source Selector */}
            <div>
              <label className="mb-2 block text-sm font-semibold text-[var(--mq-text)]">Generate From</label>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                <button type="button" onClick={() => setMode("upload")}
                  className={`group flex items-center gap-3 rounded-xl border px-4 py-3.5 text-left transition ${mode === "upload" ? "border-[var(--mq-primary)] bg-[var(--mq-primary-light)] shadow-sm" : "border-[var(--mq-border)] bg-[var(--mq-surface)] hover:border-[var(--mq-text-muted)]"}`}>
                  <div className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg transition ${mode === "upload" ? "bg-[var(--mq-primary)] text-white" : "bg-[var(--mq-bg)] text-[var(--mq-text-secondary)] group-hover:text-[var(--mq-primary)]"}`}>
                    <Upload size={17} />
                  </div>
                  <div className="min-w-0">
                    <p className={`text-sm font-medium ${mode === "upload" ? "text-[var(--mq-primary)]" : "text-[var(--mq-text)]"}`}>Upload PDF / DOCX</p>
                    <p className="text-xs text-[var(--mq-text-secondary)]">Upload new file</p>
                  </div>
                </button>
                <button type="button" onClick={() => setMode("topic")}
                  className={`group flex items-center gap-3 rounded-xl border px-4 py-3.5 text-left transition ${mode === "topic" ? "border-[var(--mq-ai)] bg-[var(--mq-ai-light)] shadow-sm" : "border-[var(--mq-border)] bg-[var(--mq-surface)] hover:border-[var(--mq-text-muted)]"}`}>
                  <div className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg transition ${mode === "topic" ? "bg-[var(--mq-ai)] text-white" : "bg-[var(--mq-bg)] text-[var(--mq-text-secondary)] group-hover:text-[var(--mq-ai)]"}`}>
                    <MessageSquare size={17} />
                  </div>
                  <div className="min-w-0">
                    <p className={`text-sm font-medium ${mode === "topic" ? "text-[var(--mq-ai)]" : "text-[var(--mq-text)]"}`}>Topic / Prompt</p>
                    <p className="text-xs text-[var(--mq-text-secondary)]">Enter a topic</p>
                  </div>
                </button>
                <button type="button" onClick={() => setMode("material")}
                  className={`group flex items-center gap-3 rounded-xl border px-4 py-3.5 text-left transition ${mode === "material" ? "border-[var(--mq-primary)] bg-[var(--mq-primary-light)] shadow-sm" : "border-[var(--mq-border)] bg-[var(--mq-surface)] hover:border-[var(--mq-text-muted)]"}`}>
                  <div className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg transition ${mode === "material" ? "bg-[var(--mq-primary)] text-white" : "bg-[var(--mq-bg)] text-[var(--mq-text-secondary)] group-hover:text-[var(--mq-primary)]"}`}>
                    <BookOpen size={17} />
                  </div>
                  <div className="min-w-0">
                    <p className={`text-sm font-medium ${mode === "material" ? "text-[var(--mq-primary)]" : "text-[var(--mq-text)]"}`}>Study Material</p>
                    <p className="text-xs text-[var(--mq-text-secondary)]">Choose from Vault</p>
                  </div>
                </button>
              </div>
            </div>

{/* Material selector */}
            {mode === "material" && (
              <div>
                <label className="mb-2 block text-sm font-semibold text-[var(--mq-text)]">Study Material</label>
                <div className="relative" ref={materialRef}>
                  <button
                    type="button"
                    onClick={() => {
                      setMaterialOpen(!materialOpen);
                      if (!materialOpen) {
                        setTimeout(() => materialSearchRef.current?.focus(), 100);
                      }
                    }}
                    disabled={materialsLoading}
                    className="flex h-12 w-full items-center justify-between rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-4 text-sm text-[var(--mq-text)] outline-none transition focus:border-[var(--mq-primary)] focus:ring-4 focus:ring-[var(--mq-primary)]/10 disabled:opacity-50"
                  >
                    <span className={currentMaterial ? "" : "text-[var(--mq-text-muted)]"}>
                      {materialsLoading ? "Loading materials..." : currentMaterial?.title ?? "Select a material"}
                    </span>
                    <ChevronDown size={18} className={`text-[var(--mq-text-secondary)] transition-transform ${materialOpen ? "rotate-180" : ""}`} />
                  </button>

                  {materialOpen && (
                    <div className="absolute z-20 mt-1 w-full overflow-hidden rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] shadow-lg">
                      {/* Search inside dropdown */}
                      <div className="border-b border-[var(--mq-border)] p-2">
                        <div className="relative">
                          <Search size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-[var(--mq-text-muted)]" />
                          <input
                            ref={materialSearchRef}
                            type="text"
                            value={materialSearch}
                            onChange={(e) => setMaterialSearch(e.target.value)}
                            placeholder="Search materials..."
                            className="h-9 w-full rounded-lg border border-[var(--mq-border)] bg-[var(--mq-bg)] pl-8 pr-8 text-sm outline-none focus:border-[var(--mq-primary)] focus:bg-[var(--mq-surface)]"
                          />
                          {materialSearch && (
                            <button onClick={() => setMaterialSearch("")} className="absolute right-2 top-1/2 -translate-y-1/2 text-[var(--mq-text-muted)] hover:text-[var(--mq-text-secondary)]">
                              <X size={14} />
                            </button>
                          )}
                        </div>
                      </div>

                      {/* Material list */}
                      <div className="max-h-60 overflow-y-auto p-1">
                        {filteredMaterials.length === 0 ? (
                          <div className="px-3 py-6 text-center text-sm text-[var(--mq-text-secondary)]">
                            {materials.length === 0 ? "No materials yet" : "No matching materials"}
                          </div>
                        ) : (
                          filteredMaterials.map((m) => (
                            <button
                              key={m.id}
                              type="button"
                              onClick={() => {
                                setSelectedMaterial(m.id);
                                setMaterialOpen(false);
                                setMaterialSearch("");
                              }}
                              className={`flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left transition ${
                                selectedMaterial === m.id
                                  ? "bg-[var(--mq-primary-light)] text-[var(--mq-primary)]"
                                  : "text-[var(--mq-text)] hover:bg-[var(--mq-bg)]"
                              }`}
                            >
                              <FileText size={16} className="shrink-0 text-[var(--mq-text-secondary)]" />
                              <span className="flex-1 truncate text-sm">{m.title}</span>
                              {selectedMaterial === m.id && <Check size={16} className="shrink-0 text-[var(--mq-primary)]" />}
                            </button>
                          ))
                        )}
                      </div>
                    </div>
                  )}
                </div>
                {currentMaterial && <div className="mt-3 flex items-center gap-2 text-xs text-[var(--mq-text-secondary)]"><FileText size={14} /> Selected from your Knowledge Vault</div>}
                {materials.length === 0 && !materialsLoading && <p className="mt-2 text-xs text-[var(--mq-text-secondary)]">Upload a material first, or switch to Topic mode.</p>}
              </div>
            )}

            
            {/* Upload Document -- Drag & Drop + Click */}
            {mode === "upload" && (
              <div>
                <label className="mb-2 block text-sm font-semibold text-[var(--mq-text)]">Upload your study material</label>
                <div
                  className={`relative rounded-xl border-2 border-dashed p-8 text-center transition ${uploadFile ? "border-[var(--mq-success)] bg-[var(--mq-success-light)]/30" : "border-[var(--mq-border)] bg-[var(--mq-bg)] hover:border-[var(--mq-primary)]"}`}
                  onDragOver={(e) => { e.preventDefault(); e.stopPropagation(); }}
                  onDragLeave={(e) => { e.preventDefault(); e.stopPropagation(); }}
                  onDrop={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    const file = e.dataTransfer.files?.[0];
                    if (file) {
                      const ext = file.name.split('.').pop()?.toLowerCase();
                      if (ext === 'pdf' || ext === 'docx') {
                        setUploadFile(file);
                        setGenerateError("");
                      } else {
                        setGenerateError("PDF and DOCX files are supported.");
                      }
                    }
                  }}
                >
                  {uploadFile ? (
                    <div className="space-y-3">
                      <div className="flex items-center justify-center gap-3">
                        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-[var(--mq-primary-light)]">
                          <FileText size={24} className="text-[var(--mq-primary)]" />
                        </div>
                        <div className="text-left">
                          <p className="text-sm font-semibold text-[var(--mq-text)]">{uploadFile.name}</p>
                          <p className="text-xs text-[var(--mq-text-secondary)]">{(uploadFile.size / 1024 / 1024).toFixed(2)} MB ??? {uploadFile.type === "application/pdf" ? "PDF" : "DOCX"}</p>
                        </div>
                      </div>
                      <button type="button" onClick={() => setUploadFile(null)} className="inline-flex items-center gap-1 rounded-lg px-3 py-1.5 text-xs font-medium text-[var(--mq-error)] transition hover:bg-[var(--mq-error-light)]">
                        <X size={12} /> Remove
                      </button>
                    </div>
                  ) : (
                    <label className="cursor-pointer block">
                      <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-[var(--mq-primary-light)]">
                        <Upload size={24} className="text-[var(--mq-primary)]" />
                      </div>
                      <p className="text-sm font-semibold text-[var(--mq-text)]">Upload your study material</p>
                      <p className="mt-1.5 text-xs text-[var(--mq-text-secondary)]">PDF or DOCX ??? drag and drop or click to browse</p>
                      <input
                        type="file"
                        className="hidden"
                        accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        onChange={(e) => {
                          const file = e.target.files?.[0];
                          if (file) {
                            const ext = file.name.split('.').pop()?.toLowerCase();
                            if (ext !== 'pdf' && ext !== 'docx') {
                              setGenerateError("PDF and DOCX files are supported.");
                              return;
                            }
                            setUploadFile(file);
                            setGenerateError("");
                          }
                        }}
                      />
                    </label>
                  )}
                </div>
                {uploading && (
                  <div className="mt-3 flex items-center gap-2 text-sm text-[var(--mq-primary)]">
                    <Loader2 size={16} className="animate-spin" />
                    <span>{uploadProgress || "Uploading..."}</span>
                  </div>
                )}
              </div>
            )}

{/* Topic input */}
            {mode === "topic" && (
              <div>
                <label className="mb-2 block text-sm font-semibold text-[var(--mq-text)]">Topic or Prompt</label>
                <textarea value={topicText} onChange={(e) => setTopicText(e.target.value)}
                  placeholder="e.g. Java Collections framework, ArrayList vs LinkedList, HashMap internals..."
                  rows={5} maxLength={5000}
                  className="w-full rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-4 py-3 text-sm text-[var(--mq-text)] outline-none transition placeholder:text-[var(--mq-text-secondary)] focus:border-[var(--mq-ai)] focus:ring-4 focus:ring-[var(--mq-ai)]/10" />
                <div className="mt-2 flex items-center justify-between text-xs text-[var(--mq-text-muted)]">
                  <span>Describe what you want to be quizzed on</span>
                  <span>{topicText.length}/5000</span>
                </div>
              </div>
            )}

            {/* Question count - BUTTONS (only for MCQ) */}
            {selectedTool === "mcq" && (
              <div>
                <label className="mb-2 block text-sm font-semibold text-[var(--mq-text)]">Number of Questions</label>
                <div className="grid grid-cols-4 gap-3">
                  {questionCounts.map((count) => (
                    <button key={count} type="button" onClick={() => setQuestionCount(count)}
                      className={`rounded-xl border px-4 py-3 text-sm font-medium transition ${questionCount === count ? "border-[var(--mq-primary)] bg-[var(--mq-primary-light)] text-[var(--mq-primary)]" : "border-[var(--mq-border)] bg-[var(--mq-surface)] text-[var(--mq-text-secondary)] hover:border-[var(--mq-text-muted)] hover:text-[var(--mq-text)]"}`}>
                      {count}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Difficulty (only for MCQ) */}
            {selectedTool === "mcq" && (
              <div>
                <label className="mb-2 block text-sm font-semibold text-[var(--mq-text)]">Difficulty</label>
                <div className="grid grid-cols-3 gap-3">
                  {difficulties.map((level) => (
                    <button key={level.value} type="button" onClick={() => setDifficulty(level.value)}
                      className={`rounded-xl border px-4 py-3 text-sm font-medium transition ${difficulty === level.value ? "border-[var(--mq-primary)] bg-[var(--mq-primary-light)] text-[var(--mq-primary)]" : "border-[var(--mq-border)] bg-[var(--mq-surface)] text-[var(--mq-text-secondary)] hover:border-[var(--mq-text-muted)] hover:text-[var(--mq-text)]"}`}>
                      {level.label}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Quiz Mode (only for MCQ) */}
            {selectedTool === "mcq" && (
              <div>
                <label className="mb-2 block text-sm font-semibold text-[var(--mq-text)]">Quiz Mode</label>
                <div className="grid grid-cols-3 gap-3">
                  {quizModes.map((qm) => (
                    <button key={qm.value} type="button" onClick={() => setQuizDeliveryMode(qm.value)}
                      className={`rounded-xl border px-3 py-3 text-left transition ${quizDeliveryMode === qm.value ? "border-[var(--mq-primary)] bg-[var(--mq-primary-light)]" : "border-[var(--mq-border)] bg-[var(--mq-surface)] hover:border-[var(--mq-text-muted)]"}`}>
                      <div className="flex items-center gap-2">
                        <span className="text-base">{qm.icon}</span>
                        <span className={`text-sm font-medium ${quizDeliveryMode === qm.value ? "text-[var(--mq-primary)]" : "text-[var(--mq-text)]"}`}>{qm.label}</span>
                      </div>
                      <p className="mt-1 text-xs text-[var(--mq-text-secondary)]">{qm.desc}</p>
                    </button>
                  ))}
                </div>
              </div>
            )}
            {models.length > 0 && (
              <ModelSelector
                value={selectedModel || null}
                onChange={(code) => setSelectedModel(code || "")}
                recommendedModel={getModelRecommendation(difficulty)}
              />
            )}

            {/* Reasoning - Advanced Settings */}
            <div className="border-t border-[var(--mq-border)] pt-4">
              <button
                type="button"
                onClick={() => setShowAdvanced(!showAdvanced)}
                className="flex items-center gap-2 text-sm font-medium text-[var(--mq-text-secondary)] hover:text-[var(--mq-text)]"
              >
                <span>Advanced Settings</span>
                <ChevronDown size={16} className={`transition-transform ${showAdvanced ? "rotate-180" : ""}`} />
              </button>
              {showAdvanced && (
                <div className="mt-4">
                  <ReasoningSelector
                    value={reasoningEffort}
                    onChange={setReasoningEffort}
                    modelCode={selectedModel || null}
                  />
                </div>
              )}
            </div>

          </div>

          {generateError && (
            <div className="mt-6 rounded-xl border border-[var(--mq-error-border)] bg-[var(--mq-error-light)] px-4 py-3 text-sm text-[var(--mq-error)]">{generateError}</div>
          )}

          {toolResult && (
            <div className="mt-6 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-bg)] p-5">
              <p className="mb-2 text-xs font-semibold uppercase text-[var(--mq-text-secondary)]">Result</p>
              <pre className="max-h-96 overflow-auto whitespace-pre-wrap text-sm text-[var(--mq-text)]">{toolResult}</pre>
            </div>
          )}

          <div className="mt-8 border-t border-[var(--mq-border)] pt-6">
            {/* Generation progress */}
            {isGenerating && generationStage && (
              <div
                role="status"
                aria-live="polite"
                aria-atomic="true"
                className="mb-4 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-bg)] p-4"
              >
                <div className="flex items-center gap-3">
                  <Loader2 size={18} className="animate-spin text-[var(--mq-primary)]" />
                  <span className="text-sm font-medium text-[var(--mq-text)]">{generationStage}</span>
                </div>
                <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-[var(--mq-border)]">
                  <div className="h-full rounded-full bg-gradient-to-r from-[var(--mq-primary)] to-[var(--mq-ai)] transition-all duration-500" style={{ width: generationStage === "Complete!" ? "100%" : "60%" }} />
                </div>
              </div>
            )}

            <button onClick={handleGenerate} disabled={isGenerating || !canGenerate}
              className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-[var(--mq-primary)] to-[var(--mq-ai)] text-sm font-semibold text-white shadow-sm transition hover:opacity-95 disabled:cursor-not-allowed disabled:opacity-60">
              {isGenerating ? <><Loader2 size={18} className="animate-spin" /> Generating...</> : <><Sparkles size={18} /> {selectedTool === "mcq" ? "Generate MCQs" : selectedTool === "summarize" ? "Summarize" : selectedTool === "flashcards" ? "Generate Flashcards" : "Generate Notes"}</>}
            </button>
          </div>
        </section>

        <aside className="space-y-5">
          <div className="overflow-hidden rounded-2xl bg-gradient-to-br from-[var(--mq-primary)] to-[var(--mq-ai)] p-6 text-white">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-white/15 dark:bg-white/20"><Sparkles size={21} /></div>
            <h3 className="mt-5 text-lg font-semibold">AI-powered practice</h3>
            <p className="mt-2 text-sm leading-6 text-white/80">
              {mode === "material" ? "MindQ analyzes your study material and creates questions based on the selected difficulty and question count." : "Type any topic and MindQ will generate questions from its knowledge. Great for quick revision!"}
            </p>
          </div>

          <div className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[var(--mq-bg)] text-[var(--mq-primary)]">
                {mode === "upload" ? <Upload size={19} /> : mode === "material" ? <BookOpen size={19} /> : <MessageSquare size={19} />}
              </div>
              <div>
                <p className="text-xs text-[var(--mq-text-secondary)]">{mode === "upload" ? "Uploaded file" : mode === "material" ? "Selected material" : "Topic"}</p>
                <p className="mt-0.5 text-sm font-semibold text-[var(--mq-text)]">
                  {mode === "upload"
                    ? uploadFile ? uploadFile.name : "No file selected"
                    : mode === "material"
                      ? currentMaterial?.title ?? (materialsLoading ? "Loading..." : "No material selected")
                      : topicText.trim() ? (topicText.trim().length > 40 ? topicText.trim().slice(0, 40) + "..." : topicText.trim()) : "Enter a topic above"}
                </p>
              </div>
            </div>
            <div className="mt-5 space-y-3 border-t border-[var(--mq-border)] pt-5">
              <div className="flex justify-between text-sm"><span className="text-[var(--mq-text-secondary)]">Questions</span><span className="font-medium text-[var(--mq-text)]">{questionCount}</span></div>
              <div className="flex justify-between text-sm"><span className="text-[var(--mq-text-secondary)]">Difficulty</span><span className="font-medium text-[var(--mq-text)]">{currentDifficulty?.label ?? difficulty}</span></div>
              <div className="flex justify-between text-sm"><span className="text-[var(--mq-text-secondary)]">Source</span><span className="font-medium text-[var(--mq-text)]">{mode === "upload" ? "From Upload" : mode === "material" ? "From Material" : "From Topic"}</span></div>
              <div className="flex justify-between text-sm"><span className="text-[var(--mq-text-secondary)]">Quiz Mode</span><span className="font-medium text-[var(--mq-text)]">{quizModes.find((q) => q.value === quizDeliveryMode)?.label ?? quizDeliveryMode}</span></div>
              <div className="flex justify-between text-sm"><span className="text-[var(--mq-text-secondary)]">Model</span><span className="font-medium text-[var(--mq-text)]">{getModelOption(selectedModel || null).role} ({getModelOption(selectedModel || null).name})</span></div>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}
