<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick } from 'vue';
import { ElForm, ElFormItem, ElInput } from 'element-plus';
import BaseButton from '@/components/BaseButton.vue';
import BaseInput from '@/components/BaseInput.vue';
import BaseSelect from '@/components/BaseSelect.vue';
import type { RoleVO, RoleCreateRequest, RoleUpdateRequest } from '@/types/role';

interface Props {
  visible: boolean;
  mode: 'create' | 'edit';
  initialData?: RoleVO | null;
}

interface Emits {
  submit: [data: RoleCreateRequest | RoleUpdateRequest];
  close: [];
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  mode: 'create',
  initialData: null,
});

const emit = defineEmits<Emits>();

const formRef = ref<InstanceType<typeof ElForm>>();

const form = reactive<RoleCreateRequest>({
  code: '',
  name: '',
  description: '',
  status: 1,
});

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
];

const rules = {
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^ROLE_[A-Z0-9_]+$/, message: '角色编码格式不正确，必须以 ROLE_ 开头且仅包含大写字母、数字、下划线', trigger: 'blur' },
    { max: 50, message: '角色编码长度不能超过50位', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请输入角色名', trigger: 'blur' },
    { max: 50, message: '角色名长度不能超过50位', trigger: 'blur' },
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' },
  ],
};

const isEditMode = computed(() => props.mode === 'edit');
const drawerTitle = computed(() => isEditMode.value ? '编辑角色' : '新增角色');

const resetForm = () => {
  form.code = '';
  form.name = '';
  form.description = '';
  form.status = 1;
  formRef.value?.clearValidate();
};

const fillForm = (data: RoleVO) => {
  form.code = data.code;
  form.name = data.name;
  form.description = data.description || '';
  form.status = data.status;
};

const handleSubmit = () => {
  formRef.value?.validate((valid) => {
    if (valid) {
      if (isEditMode.value) {
        const { code, ...updateData } = form;
        emit('submit', updateData as RoleUpdateRequest);
      } else {
        emit('submit', form as RoleCreateRequest);
      }
    }
  });
};

const handleClose = () => {
  emit('close');
};

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      resetForm();
      if (isEditMode.value && props.initialData) {
        fillForm(props.initialData);
      }
      nextTick(() => {
        formRef.value?.clearValidate();
      });
    } else {
      resetForm();
    }
  },
  { immediate: true }
);

watch(
  () => props.initialData,
  (newVal) => {
    if (isEditMode.value && newVal && props.visible) {
      fillForm(newVal);
    }
  }
);

defineOptions({
  name: 'RoleFormDrawer',
});
</script>

<template>
  <el-drawer
    v-model="props.visible"
    :title="drawerTitle"
    direction="rtl"
    size="480px"
    :before-close="handleClose"
    @close="emit('close')"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="80px"
      class="role-form"
    >
      <el-form-item
        v-if="!isEditMode"
        label="角色编码"
        prop="code"
      >
<BaseInput
           v-model="form.code"
           placeholder="请输入角色编码 (如 ROLE_ADMIN)"
           :maxlength="50"
         />
      </el-form-item>

      <el-form-item
        v-else
        label="角色编码"
      >
        <BaseInput
          v-model="form.code"
          placeholder="角色编码不可修改"
          readonly
          disabled
        />
      </el-form-item>

      <el-form-item label="角色名" prop="name">
<BaseInput
           v-model="form.name"
           placeholder="请输入角色名"
           :maxlength="50"
         />
      </el-form-item>

      <el-form-item label="描述" prop="description">
<ElInput
           v-model="form.description"
           type="textarea"
           :rows="3"
           placeholder="请输入描述 (可选)"
           :maxlength="200"
           style="width: 100%"
         />
      </el-form-item>

      <el-form-item label="状态" prop="status">
<BaseSelect
           v-model="form.status as number | null"
           :options="statusOptions"
           placeholder="请选择状态"
         />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="role-form__footer">
        <BaseButton variant="default" @click="handleClose">取消</BaseButton>
        <BaseButton variant="primary" @click="handleSubmit">确定</BaseButton>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped lang="scss">
.role-form {
  padding: 8px 4px 0;
}

.role-form__footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  margin-top: 8px;
  border-top: 1px solid var(--color-border-light);
}

:deep(.el-drawer__body) {
  padding: 0 24px 24px;
}

:deep(.el-drawer__header) {
  padding: 16px 24px;
  border-bottom: 1px solid var(--color-border-light);
}

:deep(.el-drawer__title) {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}
</style>